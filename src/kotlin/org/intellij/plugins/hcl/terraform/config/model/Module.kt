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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.PsiElementProcessor
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns
import java.util.*

class Module private constructor(val item: PsiFileSystemItem) {
  companion object {
    private val LOG = Logger.getInstance(Module::class.java)

    fun getModule(file: PsiFile): Module {
      val directory = file.containingDirectory
      if (directory == null) {
        // File only in-memory, assume as only file in module
        return Module(file as HCLFile)
      } else {
        return Module(directory)
      }
    }

    fun getAsModuleBlock(moduleBlock: HCLBlock): Module? {
      return ModuleDetectionUtil.getAsModuleBlock(moduleBlock);
    }

    private class CollectVariablesVisitor : HCLElementVisitor() {
      val collected: MutableSet<Pair<Variable, HCLBlock>> = HashSet()
      override fun visitBlock(o: HCLBlock) {
        if ("variable" != o.getNameElementUnquoted(0)) return
        val name = o.getNameElementUnquoted(1) ?: return

        val props = TypeModel.Variable.properties.map { p ->
          if (p is PropertyType) {
            return@map o.`object`?.findProperty(p.name)?.toProperty(p)
          }
          return@map null
        }.filterNotNull().map { it.toPOB() }
        collected.add(Pair(Variable(name, *props.toTypedArray()), o))
      }
    }

    private class CollectLocalsVisitor : HCLElementVisitor() {
      val collected: MutableSet<Pair<String, HCLProperty>> = HashSet()
      override fun visitBlock(o: HCLBlock) {
        if (!TerraformPatterns.LocalsRootBlock.accepts(o)) return

        val propList = o.`object`?.propertyList ?: return

        propList.mapTo(collected) { it.name to it }
      }
    }
  }

  constructor(file: HCLFile) : this(file as PsiFileSystemItem)

  constructor(directory: PsiDirectory) : this(directory as PsiFileSystemItem)

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

  fun getAllLocals(): List<Pair<String, HCLProperty>> {
    val visitor = CollectLocalsVisitor()
    process(PsiElementProcessor { file -> file.acceptChildren(visitor); true })
    return visitor.collected.toList()
  }

  fun findLocal(name: String): Pair<String, HCLProperty>? {
    val visitor = CollectLocalsVisitor()
    process(PsiElementProcessor { file -> file.acceptChildren(visitor); true })
    return visitor.collected.firstOrNull { it.first == name }
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
          if ("resource" != o.getNameElementUnquoted(0)) return
          val t = o.getNameElementUnquoted(1) ?: return
          if (type != null && type != t) return
          val n = o.getNameElementUnquoted(2) ?: return
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
          if ("data" != o.getNameElementUnquoted(0)) return
          val t = o.getNameElementUnquoted(1) ?: return
          if (type != null && type != t) return
          val n = o.getNameElementUnquoted(2) ?: return
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
          if ("provider" != o.getNameElementUnquoted(0)) return
          val tp = o.getNameElementUnquoted(1) ?: return
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
          val fqn = o.getProviderFQName() ?: return
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
          if ("module" != o.getNameElementUnquoted(0)) return
          val n = o.getNameElementUnquoted(1) ?: return
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
          if ("module" != o.getNameElementUnquoted(0)) return
          o.getNameElementUnquoted(1) ?: return
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
          if ("output" != o.getNameElementUnquoted(0)) return
          o.getNameElementUnquoted(1) ?: return
          found.add(o)
        }
      }); true
    })
    return found
  }

  fun getDefinedVariables(): List<HCLBlock> {
    val found = ArrayList<HCLBlock>()
    process(PsiElementProcessor { file ->
      file.acceptChildren(object : HCLElementVisitor() {
        override fun visitBlock(o: HCLBlock) {
          if ("variable" != o.getNameElementUnquoted(0)) return
          o.getNameElementUnquoted(1) ?: return
          found.add(o)
        }
      }); true
    })
    return found
  }

  val model: TypeModel
    get() = TypeModelProvider.getModel(item.project)

}
