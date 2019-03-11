/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.hcl.terraform.config.model

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.array
import com.beust.klaxon.string
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.DotTerraformUtil

object ModuleDetectionUtil {
  private val LOG = Logger.getInstance(ModuleDetectionUtil::class.java)

  data class ModulesManifest(val context: VirtualFile, val modules: List<ModuleManifest>)

  /**
   * @param[source] value of Module's `source` property
   * @param[dir] path relative to project root, usually starts with '.terraform/modules/'
   * @param[root] path inside `dir` directory
   */
  data class ModuleManifest(val source: String, val key: String, val version: String, val dir: String, val root: String) {
    val full
      get() = dir + if (root.isNotEmpty()) "/$root" else ""
  }

  fun getAsModuleBlock(moduleBlock: HCLBlock): Module? {
    return CachedValuesManager.getCachedValue(moduleBlock, ModuleCachedValueProvider(moduleBlock))?.first
  }

  fun getAsModuleBlockOrError(moduleBlock: HCLBlock): Pair<Module?, String?> {
    return CachedValuesManager.getCachedValue(moduleBlock, ModuleCachedValueProvider(moduleBlock))
  }

  class ModuleCachedValueProvider(private val block: HCLBlock) : CachedValueProvider<Pair<Module?, String?>> {
    override fun compute(): CachedValueProvider.Result<Pair<Module?, String?>>? {
      return doGetAsModuleBlock(block)
    }
  }

  private fun doGetAsModuleBlock(moduleBlock: HCLBlock): CachedValueProvider.Result<Pair<Module?, String?>> {
    val name = moduleBlock.getNameElementUnquoted(1) ?: return CachedValueProvider.Result(null to null, moduleBlock)
    val sourceVal = moduleBlock.`object`?.findProperty("source")?.value as? HCLStringLiteral
        ?: return CachedValueProvider.Result(null to "No 'source' property", moduleBlock)
    val source = sourceVal.value

    val file = moduleBlock.containingFile.originalFile
    val directory = file.containingDirectory ?: return CachedValueProvider.Result(null to null, moduleBlock)
    var err: String? = null

    val project = moduleBlock.project

    val dotTerraform = DotTerraformUtil.findTerraformDir(directory)
    if (dotTerraform != null) {
      LOG.debug("Found .terraform directory: $dotTerraform")
      val manifestFile = getTerraformModulesManifestFile(project, dotTerraform)
      if (manifestFile != null) {
        LOG.debug("Found manifest.json: $manifestFile")
        val manifest = CachedValuesManager.getManager(project).getCachedValue(file, ManifestCachedValueProvider(manifestFile))
        if (manifest != null) {
          val keyPrefix: String
          LOG.debug("All modules from modules.json: ${manifest.modules}")
          val pair = getKeyPrefix(directory, dotTerraform, manifest, name, source)
          if (pair.first == null) {
            val relativeModule = findRelativeModule(directory, moduleBlock, source)
            return CachedValueProvider.Result(relativeModule to (pair.second
                ?: "Can't determine key prefix"), moduleBlock, dotTerraform, manifestFile, *getModuleFiles(relativeModule))
          }
          keyPrefix = pair.first!!

          LOG.debug("Searching for module with source '$source' and keyPrefix '$keyPrefix'")
          val module = manifest.modules.find {
            it.source == source && it.key.startsWith(keyPrefix) && !it.key.removePrefix(keyPrefix).contains('|')
          }

          if (module != null) {
            LOG.debug("Found module $module")
            val path = module.full
            var relative = manifest.context.findFileByRelativePath(path)
            if (relative != null) {
              LOG.debug("Absolute module dir: $relative")
              if (isRelativeSource(source)) {
                findRelativeModule(directory, moduleBlock, source)?.let {
                  LOG.debug("Shortcutting to relative module")
                  return CachedValueProvider.Result(it to null, moduleBlock, directory, dotTerraform, manifestFile, relative, getModuleFiles(it))
                }
              }
              val canonical = relative.canonicalFile
              // TODO: Symlink resolving probably would not work on Windows
              if (canonical != null && canonical != relative) {
                if (VfsUtilCore.isAncestor(project.baseDir ?: manifest.context, canonical, true)) {
                  LOG.debug("Replacing module relative path ('${relative.name}') with canonical: '$canonical'")
                  relative = canonical
                }
              }
              val dir = PsiManager.getInstance(project).findDirectory(relative)
              if (dir != null) {
                LOG.debug("Module search succeed, directory is $dir")
                val mod = Module(dir)
                return CachedValueProvider.Result(mod to null, moduleBlock, directory, dotTerraform, manifestFile, relative, getModuleFiles(mod))
              } else {
                err = "Can't find PsiDirectory for $relative"
                LOG.debug(err)
              }
            } else {
              err = "Can't find relative dir '$path' in '${manifest.context}'"
              LOG.debug(err)
            }
          }
        }
        else {
          err = "Failed to parse .terraform/modules/modules.json, please rerun `terraform get`"
          LOG.warn(err)
        }
      } else {
        err = "No modules/modules.json found in .terraform directory, please run `terraform get` in appropriate place"
        LOG.warn(err)
      }
    } else {
      err = "No .terraform directory found, please run `terraform get` in appropriate place"
      LOG.warn(err)
    }

    if (err != null) {
      err = "Terraform Module '$name' with source '$source' directory not found locally, use `terraform get` to fetch modules."
      LOG.warn(err)
    }
    val relativeModule = findRelativeModule(directory, moduleBlock, source)
    return CachedValueProvider.Result(relativeModule to err, moduleBlock, directory, *getVFSChainOrVFS(directory, project), *getModuleFiles(relativeModule))
  }

  private fun isRelativeSource(source: String): Boolean {
    // TODO: Improve
    if (source.startsWith("./")) return true
    if (source.startsWith("../")) return true
    return false
  }

  private fun getModuleFiles(module: Module?): Array<out PsiElement> {
    if (module == null) return emptyArray()
    val item = module.item
    return when (item) {
      is PsiDirectory -> arrayOf(item, *item.files)
      else -> arrayOf(item)
    }
  }

  private fun getVFSChainOrVFS(directory: PsiDirectory, project: Project): Array<out ModificationTracker> {
    return getVFSChain(directory, project) ?: arrayOf(VirtualFileManager.getInstance())
  }

  private fun getVFSChain(file: PsiFileSystemItem, project: Project): Array<out VirtualFile>? {
    val base = project.baseDir ?: return null
    val start = file.virtualFile
    if (!VfsUtilCore.isAncestor(base, start, false)) {
      LOG.warn("'$file' is not under project root")
      return null
    }
    val chain = ArrayList<VirtualFile>()

    var parent: VirtualFile? = start
    while (true) {
      if (parent == null) return null
      chain.add(parent)
      if (parent == base) break
      parent = parent.parent
    }
    return chain.toTypedArray()
  }


  private fun getTerraformModulesManifestFile(project: Project, dotTerraform: VirtualFile): VirtualFile? {
    if (!dotTerraform.isValid || !dotTerraform.exists()) return null

    val file = dotTerraform.findFileByRelativePath("modules/modules.json")
    if (file == null || !file.exists() || file.isDirectory || !file.isValid) {
      return null
    }
    return file
  }

  class ManifestCachedValueProvider(private val file: VirtualFile) : CachedValueProvider<ModulesManifest> {
    override fun compute(): CachedValueProvider.Result<ModulesManifest>? {
      if (!file.isValid || !file.exists() || file.isDirectory) {
        return CachedValueProvider.Result(null, this.file)
      }
      val parsed = parseManifest(file)
      return CachedValueProvider.Result(parsed, this.file, file)
    }
  }

  private fun parseManifest(file: VirtualFile): ModulesManifest? {
    LOG.debug("Parsing manifest file $file")
    val stream = file.inputStream ?: return null
    val context = file.parent.parent.parent ?: return null
    val application = ApplicationManager.getApplication()
    try {
      val json: JsonObject? = stream.use {
        val parser = Parser()
        parser.parse(stream) as JsonObject?
      }
      if (json == null) {
        logErrorAndFailInInternalMode(application, "In file '$file' no JSON found")
        return null
      }
      return ModulesManifest(context, json.array<JsonObject>("Modules")?.map {
        ModuleManifest(
            source = it.string("Source") ?: "",
            key = it.string("Key") ?: "",
            version = it.string("Version") ?: "",
            dir = it.string("Dir") ?: "",
            root = it.string("Root") ?: ""
        )
      } ?: emptyList())
    } catch (e: Throwable) {
      logErrorAndFailInInternalMode(application, "Failed to parse file '$file'", e)
    }
    return null
  }

  private fun logErrorAndFailInInternalMode(application: Application, msg: String, e: Throwable? = null) {
    val msg2 = if (e == null) msg else "$msg: ${e.message}"
    if (e == null) ModuleDetectionUtil.LOG.error(msg2) else LOG.error(msg2, e)
    if (application.isInternal) {
      throw AssertionError(msg2, e)
    }
  }


  private fun findRelativeModule(directory: PsiDirectory, moduleBlock: HCLBlock, source: String): Module? {
    // Prefer local file paths over loaded modules.
    // TODO: Consider removing that
    // Used in tests

    val relative = directory.virtualFile.findFileByRelativePath(source) ?: return null
    if (!relative.exists() || !relative.isDirectory) return null
    return PsiManager.getInstance(moduleBlock.project).findDirectory(relative)?.let { Module(it) }
  }

  private fun getKeyPrefix(directory: PsiDirectory, dotTerraform: VirtualFile, manifest: ModuleDetectionUtil.ModulesManifest, name: String, source: String): Pair<String?, String?> {
    // Check whether current dir is a module itself
    val relative = VfsUtilCore.getRelativePath(directory.virtualFile, dotTerraform)
    if (relative != null) {
      val currentModule = manifest.modules.find { it.full == ".terraform/$relative" }
      if (currentModule != null) {
        return currentModule.key + '|' to null
      } else {
        val err = "Path '.terraform/$relative' not found among modules, either `terraform get` should be run or we're in non-referenced module, e.g. subdir of some module"
        LOG.info(err)
        return null to err
      }
    } else {
      // Module referenced from root key would be '1.$NAME;$SOURCE' or '1.$NAME;$SOURCE.$VERSION'
      return "1.$name;$source" to null
    }
  }
}