/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.testFramework.LightVirtualFile
import org.apache.commons.codec.digest.DigestUtils
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage
import org.intellij.plugins.hil.psi.ILExpression
import java.util.*

// Actual model
open class Property(val type: PropertyType, val value: Any?)
open class Block(val type: BlockType, vararg val properties: PropertyOrBlock = arrayOf())
class PropertyOrBlock(val property: Property? = null, val block: Block? = null) {
  init {
    assert(property != null || block != null);
  }
}

fun Property.toPOB(): PropertyOrBlock {
  return PropertyOrBlock(property = this)
}

fun Block.toPOB(): PropertyOrBlock {
  return PropertyOrBlock(block = this)
}

class Resource(type: ResourceType, val name: String, vararg properties: PropertyOrBlock = arrayOf()) : Block(type, *properties)
// ProviderType from name
class Provider(type: ProviderType, val name: String, vararg properties: PropertyOrBlock = arrayOf()) : Block(type, *properties)

class Variable(val name: String, vararg properties: PropertyOrBlock = arrayOf()) : Block(TypeModel.Variable, *properties) {
  fun getDefault(): Any? {
    return properties.firstOrNull { TypeModel.Variable_Default == it.property?.type }?.property?.value
  }
}


fun HCLElement.getTerraformModule(): Module {
  val file = this.containingFile.originalFile
  assert(file is HCLFile)
  return getModule(file)
}

fun getModule(file: PsiFile): Module {
  val directory = file.containingDirectory
  if (directory == null) {
    // File only in-memory, assume as only file in module
    return Module(file as HCLFile)
  } else {
    return Module(directory)
  }
}

private val LOG = Logger.getInstance(Module::class.java.name)
fun getModule(moduleBlock: HCLBlock): Module? {
  val name = moduleBlock.getNameElementUnquoted(1) ?: return null
  val sourceVal = moduleBlock.`object`?.findProperty("source")?.value ?: return null
  if (sourceVal !is HCLStringLiteral) return null
  val source = sourceVal.value

  val file = moduleBlock.containingFile.originalFile
  val directory = file.containingDirectory ?: return null

  // Prefer local file paths over loaded modules.
  // TODO: Consider removing that
  var dir: PsiDirectory? = findRelativeModule(directory, moduleBlock, source)
  if (dir != null) {
    return Module(dir)
  }

  // Hopefully user already executed `terraform get`
  dir = doFindModule(name, source, directory)
  if (dir == null) {
    LOG.warn("Terraform Module '$name' with source '$source' directory not found locally, use `terraform get` to fetch modules.")
    return null
  }
  return Module(dir)
}

private fun findRelativeModule(directory: PsiDirectory, moduleBlock: HCLBlock, source: String): PsiDirectory? {
  val relative = directory.virtualFile.findFileByRelativePath(source) ?: return null
  if (!relative.exists() || !relative.isDirectory) return null
  return PsiManager.getInstance(moduleBlock.project).findDirectory(relative)
}

private fun doFindModule(nameElementUnquoted: String, source: String, directory: PsiDirectory): PsiDirectory? {
  val md5 = computeModuleStorageName(nameElementUnquoted, source)
  val dir = directory.findSubdirectory(".terraform")?.findSubdirectory("modules")?.findSubdirectory(md5)
  return dir
}

fun computeModuleStorageName(name: String, source: String): String {
  // TODO: Improve path calculation
  val path = listOf(name).joinToString(".") { it }
  val md5 = DigestUtils.md5Hex("root.$path-$source")!!
  return md5
}

fun PsiElement.getTerraformSearchScope(): GlobalSearchScope {
  val file = this.containingFile.originalFile
  var directory = file.containingDirectory
  if (directory == null) {
    if (this is ILExpression) {
      directory = InjectedLanguageManager.getInstance(project).getTopLevelFile(this)?.containingDirectory
    }
  }
  if (directory == null) {
    // File only in-memory, assume as only file in module
    var vf: VirtualFile? = file.virtualFile
    if (vf is LightVirtualFile) {
      vf = vf.originalFile?:vf
    }
    val parent = vf?.parent ?: return GlobalSearchScope.fileScope(file)
    return GlobalSearchScopes.directoryScope(file.project, parent, false)
  } else {
    return GlobalSearchScopes.directoryScope(directory, false)
  }
}

class Module private constructor(val item: PsiFileSystemItem) {
  constructor(file: HCLFile) : this(file as PsiFileSystemItem) {
  }

  constructor(directory: PsiDirectory) : this(directory as PsiFileSystemItem) {
  }

  fun getAllVariables(): List<Pair<Variable, HCLBlock>> {
    val visitor = CollectVariablesVisitor()
    process(PsiElementProcessor { file -> file.acceptChildren(visitor); true })
    return visitor.collected.toList()
  }

  fun findVariable(name: String): Pair<Variable, HCLBlock>? {
    val visitor = CollectVariablesVisitor()
    process(PsiElementProcessor { file -> file.acceptChildren(visitor); true })
    return visitor.collected.filter { it.first.name == name }.firstOrNull()
  }

  // val helper = PsiSearchHelper.SERVICE.getInstance(position.project)
  // helper.processAllFilesWithWord()

  private fun process(processor: PsiElementProcessor<HCLFile>): Boolean {
    // TODO: Support json files (?)
    if (item is HCLFile) {
      if (item.language == TerraformLanguage) {
        return processor.execute(item)
      }
      return false
    }
    assert(item is PsiDirectory)
    return item.processChildren(PsiElementProcessor { element ->
      if (element !is HCLFile || element.language != TerraformLanguage) return@PsiElementProcessor true
      processor.execute(element)
    })
  }

  fun findResources(type: String?, name: String?): List<HCLBlock> {
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("resource" != o.getNameElementUnquoted(0)) return;
          val t = o.getNameElementUnquoted(1) ?: return
          if (type != null && type != t) return
          val n = o.getNameElementUnquoted(2) ?: return;
          if (name == null || name == n) found.add(o)
        }
      }); true
    })
    return found
  }

  fun getDeclaredResources(): List<HCLBlock> {
    return findResources(null, null)
  }

  fun findDataSource(type: String?, name: String?): List<HCLBlock> {
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("data" != o.getNameElementUnquoted(0)) return;
          val t = o.getNameElementUnquoted(1) ?: return
          if (type != null && type != t) return
          val n = o.getNameElementUnquoted(2) ?: return;
          if (name == null || name == n) found.add(o)
        }
      }); true
    })
    return found
  }

  fun getDeclaredDataSources(): List<HCLBlock> {
    return findDataSource(null, null)
  }

  // search is either 'type' or 'type.alias'
  fun findProviders(search: String): List<HCLBlock> {
    val split = search.split('.')
    val type = split[0]
    val alias = split.getOrNull(1)
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("provider" != o.getNameElementUnquoted(0)) return;
          val tp = o.getNameElementUnquoted(1) ?: return;
          val value = o.`object`?.findProperty("alias")?.value
          val als = when (value) {
            is HCLStringLiteral -> value.value
            is HCLIdentifier -> value.id
            else -> null
          }
          if (alias == null && als == null) {
            if (type == tp) found.add(o)
          } else {
            if (alias == als) found.add(o)
          }
        }
      }); true
    })
    return found
  }

  fun getDefinedProviders(): List<Pair<HCLBlock, String>> {
    val found = ArrayList<Pair<HCLBlock, String>>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("provider" != o.getNameElementUnquoted(0)) return
          val fqn = getProviderFQName(o) ?: return
          found.add(Pair(o, fqn))
        }
      }); true
    })
    return found
  }

  fun findModules(name: String): List<HCLBlock> {
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("module" != o.getNameElementUnquoted(0)) return;
          val n = o.getNameElementUnquoted(1) ?: return;
          if (name == n) found.add(o)
        }
      }); true
    })
    return found
  }

  fun getDefinedModules(): List<HCLBlock> {
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("module" != o.getNameElementUnquoted(0)) return;
          o.getNameElementUnquoted(1) ?: return;
          found.add(o)
        }
      }); true
    })
    return found
  }

  fun getDefinedOutputs(): List<HCLBlock> {
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("output" != o.getNameElementUnquoted(0)) return;
          o.getNameElementUnquoted(1) ?: return;
          found.add(o)
        }
      }); true
    })
    return found
  }

}

class CollectVariablesVisitor : HCLElementVisitor() {
  val collected: MutableSet<Pair<Variable, HCLBlock>> = HashSet();
  override fun visitBlock(o: HCLBlock) {
    if ("variable" != o.getNameElementUnquoted(0)) return;
    val name = o.getNameElementUnquoted(1) ?: return;

    val props = TypeModel.Variable.properties.map { p ->
      if (p.property != null) {
        return@map o.`object`?.findProperty(p.property.name)?.toProperty(p.property)
      }
      return@map null
    }.filterNotNull().map { it.toPOB() }
    collected.add(Pair(Variable(name, *props.toTypedArray()), o))
  }
}

fun HCLProperty.toProperty(type: PropertyType): Property {
  return Property(type, this.value)
}

fun getProviderFQName(o: HCLBlock): String? {
  val tp = o.getNameElementUnquoted(1) ?: return null
  val value = o.`object`?.findProperty("alias")?.value
  val als = when (value) {
    is HCLStringLiteral -> value.value
    is HCLIdentifier -> value.id
    else -> null
  }
  if (als != null) {
    return "$tp.$als"
  } else {
    return tp
  }
}