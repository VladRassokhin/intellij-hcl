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


package org.intellij.plugins.hil.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import org.intellij.plugins.hcl.navigation.HCLQualifiedNameProvider
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hcl.terraform.config.model.getModule
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hil.codeinsight.HILCompletionContributor
import org.intellij.plugins.hil.inspection.PsiFakeAwarePolyVariantReference
import org.intellij.plugins.hil.psi.impl.getHCLHost

object ILSelectFromSomethingReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is ILExpression) return PsiReference.EMPTY_ARRAY
    val name = getSelectFieldText(element) ?: return PsiReference.EMPTY_ARRAY

    val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return PsiReference.EMPTY_ARRAY
    if (host !is HCLElement) return PsiReference.EMPTY_ARRAY

    val parent = element.parent
    if (parent !is ILSelectExpression) return PsiReference.EMPTY_ARRAY

    if (parent.from === element && name in HILCompletionContributor.SCOPES) return PsiReference.EMPTY_ARRAY

    val expression = getGoodLeftElement(parent, element)
    @Suppress("IfNullToElvis")
    if (expression == null) {
      // v is leftmost, no idea what to do
      return PsiReference.EMPTY_ARRAY
    }

    // FIXME: Support module outputs

    val references = expression.references
    if (references.isNotEmpty()) {
      // If we select variable from resource or data provider
      // or some other element which references another property/block
      val refs = SmartList<PsiReference>()

      // Bypass references to the right of index/star
      if (isStarOrNumber(name)) return references

      for (reference in references) {
        refs.add(HCLElementLazyReference(element, false) { incompleteCode, fake ->
          val resolved = SmartList<PsiElement>()
          if (reference is PsiFakeAwarePolyVariantReference) {
            resolved.addAll(reference.multiResolve(incompleteCode, fake).map { it.element }.filterNotNull())
          } else if (reference is PsiPolyVariantReference) {
            resolved.addAll(reference.multiResolve(incompleteCode).map { it.element }.filterNotNull())
          } else {
            reference.resolve()?.let { resolved.add(it) }
          }
          val found = SmartList<HCLElement>()
          resolved.forEach { collectReferences(it, name, found, fake) }
          found
        })
      }
      return refs.toTypedArray()
    }

    // Rest logic would try to find resource or data provider by element text

    val ev = getSelectFieldText(expression) ?: return PsiReference.EMPTY_ARRAY

    if (HILCompletionContributor.ILSE_DATA_SOURCE.accepts(parent)) {
      return arrayOf(HCLElementLazyReference(element, false) { incomplete, fake ->
        val module = this.element.getHCLHost()?.getTerraformModule()
        val dataSources = module?.findDataSource(ev, getSelectFieldText(element)!!) ?: emptyList()
        dataSources.map { it.nameIdentifier as HCLElement }
      })
    }

    // TODO: get suitable resource/provider/etc
    return arrayOf(HCLElementLazyReference(element, false) { incomplete, fake ->
      val module = this.element.getHCLHost()?.getTerraformModule()
      val resources = module?.findResources(ev, getSelectFieldText(element)!!) ?: emptyList()
      resources.map { it.nameIdentifier as HCLElement }
    })
    // TODO: support 'module.MODULE_NAME.OUTPUT_NAME' references (in that or another provider)
  }

  private fun collectReferences(r: PsiElement, name: String, found: MutableList<HCLElement>, fake: Boolean) {
    when (r) {
      is HCLStringLiteral, is HCLIdentifier -> {
        val p = r.parent
        if (p is HCLBlock && p.nameIdentifier === r) {
          return collectReferences(p, name, found, fake)
        } else if (p is HCLProperty && p.nameIdentifier === r) {
          return collectReferences(p, name, found, fake)
        }
      }
      is HCLObject -> {
        val property = r.findProperty(name)
        val blocks = r.blockList.filter { it.nameElements.any { it.name == name } }.orEmpty()
        if (property != null) {
          found.add(property)
        } else if (!blocks.isEmpty()) {
          found.addAll(blocks.map { it.nameIdentifier as HCLElement })
        }
      }
      is HCLBlock -> {
        val property = r.`object`?.findProperty(name)
        val blocks = r.`object`?.blockList?.filter { it.nameElements.any { it.name == name } }.orEmpty()
        // TODO: Move this special support somewhere else
        val blockType = r.getNameElementUnquoted(0)

        val fqn = HCLQualifiedNameProvider.getQualifiedModelName(r)
        if (ServiceManager.getService(TypeModelProvider::class.java).ignored_references.contains(fqn)) {
          if (fake) found.add(FakeHCLProperty(name, r))
        } else if ("module" == blockType) {
          val module = getModule(r)
          if (module == null) {
            // Resolve everything
            if (fake) {
              found.add(FakeHCLProperty(name, r))
            }
          } else {
            val outputs = module.getDefinedOutputs().filter { it.name == name }
            if (!outputs.isEmpty()) {
              outputs.map { it.nameIdentifier as HCLElement }.toCollection(found)
            } else if (fake) {
              //              found.add(FakeHCLProperty(name))
            }
          }
        } else if ("variable" == blockType) {
          val defaultMap = r.`object`?.findProperty("default")?.value
          if (defaultMap is HCLObject) {
            collectReferences(defaultMap, name, found, fake)
          }
        } else if (property != null) {
          found.add(property)
        } else if (!blocks.isEmpty()) {
          found.addAll(blocks.map { it.nameIdentifier as HCLElement })
        } else if (fake) {
          ModelHelper.getBlockProperties(r).filter { it.name == name }.map { FakeHCLProperty(it.name, r) }.toCollection(found)
        }
      }
      is HCLProperty -> {
        if (r is FakeHCLProperty) {
          if (fake) {
            // TODO: Ugly fix until 'terraform_remote_state' properly resolved
            if (r._name == "output" && r._parent is HCLBlock) {
              if (r._parent.getNameElementUnquoted(0) == "resource"
                  && r._parent.getNameElementUnquoted(1) == "terraform_remote_state") {
                found.add(FakeHCLProperty(name, r))
              }
            } else {
              val fqn = HCLQualifiedNameProvider.getQualifiedModelName(r)
              if (ServiceManager.getService(TypeModelProvider::class.java).ignored_references.contains(fqn)) {
                found.add(FakeHCLProperty(name, r))
              }
            }
          }
        } else {
          val value = r.value
          if (value is HCLObject) {
            val property = value.findProperty(name)
            if (property != null) {
              found.add(property)
            }
          }
        }
      }
    }
  }


}

private fun getSelectFieldText(expression: ILExpression): String? {
  return when (expression) {
    is ILLiteralExpression -> expression.unquotedText
    is ILVariable -> expression.name
    else -> null
  }
}


fun getGoodLeftElement(select: ILSelectExpression, right: ILExpression, skipStars: Boolean = true): ILExpression? {
  // select = left.right
  val left = select.from
  if (left is ILSelectExpression) {
    // left = from.middle
    val middle = left.field
    val from = left.from
    if (from is ILSelectExpression && middle != null && skipStars) {
      val text = getSelectFieldText(middle)
      if (text != null && isStarOrNumber(text)) {
        // left == from.*
        // from == X.Y
        // select = X.Y.*.right
        // Y == from.field
        return from.field
      }
    }
    return middle
  }

  if (left !== right) return left
  // TODO: Investigate is that enough
  return null
}

fun isStarOrNumber(text: String) = text == "*" || text.isNumber()

fun String.isNumber(): Boolean {
  try {
    this.toInt()
    return true
  } catch(e: NumberFormatException) {
    return false
  }
}

