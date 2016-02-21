/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.psi.HCLObject
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hil.codeinsight.HILCompletionContributor
import org.intellij.plugins.hil.psi.impl.ILExpressionBase

object ILSelectFromSomethingReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is ILVariable) return PsiReference.EMPTY_ARRAY
    val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return PsiReference.EMPTY_ARRAY
    if (host !is HCLElement) return PsiReference.EMPTY_ARRAY

    val parent = element.parent
    if (parent !is ILSelectExpression) return PsiReference.EMPTY_ARRAY

    val name = element.name
    if (name in HILCompletionContributor.SCOPES) return PsiReference.EMPTY_ARRAY

    val expression = getGoodLeftElement(parent, element);
    @Suppress("IfNullToElvis")
    if (expression == null) {
      // v is leftmost, no idea what to do
      return PsiReference.EMPTY_ARRAY;
    }
    val references = expression.references
    if (references.isNotEmpty()) {
      val refs = SmartList<PsiReference>()

      for (reference in references) {
        refs.add(object : PsiReferenceBase.Poly<ILVariable>(element, true) {
          override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
            val resolved = SmartList<PsiElement>()
            if (reference is PsiPolyVariantReference) {
              resolved.addAll(reference.multiResolve(incompleteCode).map { it.element }.filterNotNull())
            } else {
              reference.resolve()?.let { resolved.add(it) }
            }
            if (resolved.isEmpty()) {
              return emptyArray()
            }
            val found = SmartList<PsiElement>()
            for (r in resolved) {
              when (r) {
                is HCLBlock -> {
                  val property = r.`object`?.findProperty(name)
                  if (property != null) {
                    found.add(property)
                  }
                }
                is HCLProperty -> {
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
            if (!found.isEmpty()) {
              return found.map { PsiElementResolveResult(it) }.toTypedArray()
            }
            return emptyArray()
          }

          override fun getVariants(): Array<out Any> {
            return EMPTY_ARRAY
          }
        })
      }
      return refs.toTypedArray()
    }

    val ev = getSelectFieldText(expression) ?: return PsiReference.EMPTY_ARRAY

    // TODO: get suitable resource/provider/etc
    return arrayOf(HCLBlockNameLazyReference(element, true, 2) {
      (this.element as ILExpressionBase).getHCLHost()?.getTerraformModule()?.findResources(ev, this.element.name)?:emptyList()
    })
    // TODO: support 'module.MODULE_NAME.OUTPUT_NAME' references (in that or another provider)
  }


}

private fun getSelectFieldText(expression: ILExpression): String? {
  return when (expression) {
    is ILLiteralExpression -> expression.unquotedText
    is ILVariable -> expression.name
    else -> null
  }
}


fun getGoodLeftElement(select: ILSelectExpression, right: ILVariable): ILExpression? {
  // select = left.right
  val left = select.from
  if (left is ILSelectExpression) {
    // left = from.middle
    val middle = left.field
    val from = left.from
    if (from is ILSelectExpression && middle != null) {
      val text = getSelectFieldText(middle)
      if (text != null && (text == "*" || text.isNumber())) {
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

fun String.isNumber(): Boolean {
  try {
    this.toInt()
    return true
  } catch(e: NumberFormatException) {
    return false
  }
}


