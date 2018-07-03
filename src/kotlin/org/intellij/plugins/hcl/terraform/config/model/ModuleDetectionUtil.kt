/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.psi.getNameElementUnquoted

object ModuleDetectionUtil {
  private val LOG = Logger.getInstance(ModuleDetectionUtil::class.java)

  data class ModulesManifest(val context: VirtualFile, val modules: List<ModuleManifest>)

  data class ModuleManifest(val source: String, val key: String, val version: String, val dir: String, val root: String) {
    val full
      get() = dir + if (root.isNotEmpty()) "/$root" else ""
  }

  fun getAsModuleBlock(moduleBlock: HCLBlock): Module? {
    val name = moduleBlock.getNameElementUnquoted(1) ?: return null
    val sourceVal = moduleBlock.`object`?.findProperty("source")?.value as? HCLStringLiteral ?: return null
    val source = sourceVal.value

    val file = moduleBlock.containingFile.originalFile
    val directory = file.containingDirectory ?: return null

    val dotTerraform = ModuleDetectionUtil.getTerraformDirSomewhere(directory)
    if (dotTerraform != null) {
      LOG.debug("Found .terraform directory: $dotTerraform")
      val manifest = ModuleDetectionUtil.getTerraformModulesManifest(file)
      val keyPrefix: String
      if (manifest != null) {
        LOG.debug("All modules from modules.json: ${manifest.modules}")
        keyPrefix = getKeyPrefix(directory, dotTerraform, manifest, name, source) ?: return findRelativeModule(directory, moduleBlock, source)

        LOG.debug("Searching for module with source '$source' and keyPrefix '$keyPrefix'")
        val module = manifest.modules.find {
          it.source == source && it.key.startsWith(keyPrefix) && !it.key.removePrefix(keyPrefix).contains('|')
        }

        if (module != null) {
          LOG.debug("Found module $module")
          val path = module.full
          val relative = manifest.context.findFileByRelativePath(path)
          if (relative != null) {
            LOG.debug("Absolute module dir: $relative")
            val dir = PsiManager.getInstance(moduleBlock.project).findDirectory(relative)
            if (dir != null) {
              LOG.debug("Module search succeed, directory is $dir")
              return Module(dir)
            } else {
              LOG.debug("Can't find PsiDirectory for $relative")
            }
          } else {
            LOG.debug("Can't find relative dir '$path' in '${manifest.context}'")
          }
        }
      }
    } else {
      LOG.warn("No .terraform found under project directory, please run `terraform get` in appropriate place")
      return findRelativeModule(directory, moduleBlock, source)
    }

    findRelativeModule(directory, moduleBlock, source)

    LOG.warn("Terraform Module '$name' with source '$source' directory not found locally, use `terraform get` to fetch modules.")
    return null
  }


  private val TerraformModulesManifestKey = Key<ModulesManifest>("TerraformModulesManifest")
  private val TerraformModulesManifestFileUrlKey = Key<String>("TerraformModulesManifestFileUrl")

  fun getTerraformModulesManifest(file: PsiFile): ModulesManifest? {
    val directory = file.containingDirectory ?: return null

    var url = directory.getUserData(TerraformModulesManifestFileUrlKey)
    var manifestFile: VirtualFile? = null

    if (url != null) {
      manifestFile = VirtualFileManager.getInstance().findFileByUrl(url)
      if (manifestFile == null) {
        directory.putUserData(TerraformModulesManifestFileUrlKey, null)
      }
    }

    if (manifestFile == null) {
      val dotTerraformDir = getTerraformDirSomewhere(directory) ?: return null
      manifestFile = dotTerraformDir.findFileByRelativePath("modules/modules.json") ?: return null
      if (!manifestFile.exists() || manifestFile.isDirectory) return null

      url = manifestFile.url
      directory.putUserData(TerraformModulesManifestFileUrlKey, url)
    }

    manifestFile.getUserData(TerraformModulesManifestKey)?.let { return it }
    val parsed = parseManifest(manifestFile)
    manifestFile.putUserData(TerraformModulesManifestKey, parsed)
    return parsed
  }

  private fun parseManifest(file: VirtualFile): ModulesManifest? {
    val stream = file.inputStream ?: return null
    val context = file.parent.parent.parent ?: return null
    val application = ApplicationManager.getApplication()
    val json: JsonObject?
    try {
      json = stream.use {
        val parser = Parser()
        parser.parse(stream) as JsonObject?
      }
      if (json == null) {
        logErrorAndFailInInternalMode(application, "In file '$file' no JSON found")
        return null
      }
    } catch (e: Exception) {
      logErrorAndFailInInternalMode(application, "Failed to load json data from file '$file'", e)
      return null
    }
    try {
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

  private fun getKeyPrefix(directory: PsiDirectory, dotTerraform: VirtualFile, manifest: ModuleDetectionUtil.ModulesManifest, name: String, source: String): String? {
    // Check whether current dir is a module itself
    val relative = VfsUtilCore.getRelativePath(directory.virtualFile, dotTerraform)
    if (relative != null) {
      val currentModule = manifest.modules.find { it.full == ".terraform/$relative" }
      if (currentModule != null) {
        return currentModule.key + '|'
      } else {
        LOG.info("Path '.terraform/$relative' not found among modules, either `terraform get` should be run or we're in non-referenced module, e.g. subdir of some module")
        return null
      }
    } else {
      // Module referenced from root key would be '1.$NAME;$SOURCE' or '1.$NAME;$SOURCE.$VERSION'
      return "1.$name;$source"
    }
  }

  private fun getTerraformDirSomewhere(file: PsiDirectory): VirtualFile? {
    val base = file.project.baseDir ?: return null
    val start = file.virtualFile
    if (!VfsUtilCore.isAncestor(base, start, false)) {
      LOG.warn("File $file is not under project root")
      return null
    }
    var parent: VirtualFile? = start
    while (true) {
      if (parent == null) return null
      if (parent == base) break

      val child = parent.findChild(".terraform")
      if (child != null && child.isDirectory) {
        return child
      }
      parent = parent.parent
    }
    return null
  }
}