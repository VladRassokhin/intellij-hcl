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


package org.intellij.plugins.hcl.terraform.il.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.psi.HCLObject
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.il.codeinsight.TILCompletionProvider
import java.util.*

object ILSelectFromSomethingReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is ILVariable) return PsiReference.EMPTY_ARRAY
    val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return PsiReference.EMPTY_ARRAY
    if (host !is HCLElement) return PsiReference.EMPTY_ARRAY

    val parent = element.parent
    if (parent !is ILSelectExpression) return PsiReference.EMPTY_ARRAY

    val name = element.name

    val expression = getGoodLeftElement(parent, element);
    if (expression == null) {
      // v is leftmost, no idea what to do
      return PsiReference.EMPTY_ARRAY;
    }
    val references = expression.references
    if (references.isNotEmpty()) {
      val resolved = references.map {
        if (it is HCLBlockNameReference) {
          it.block
        } else if (it is HCLBlockPropertyReference) {
          it.property
        } else {
          it.resolve()
        }
      }.filterNotNull()

      val found = ArrayList<PsiReference>()
      for (r in resolved) {
        when (r) {
          is HCLBlock -> {
            val property = r.`object`?.findProperty(name)
            if (property != null) {
              found.add(PsiReferenceBase.Immediate(element, true, property))
            }
          }
          is HCLProperty -> {
            val value = r.value
            if (value is HCLObject) {
              val property = value.findProperty(name)
              if (property != null) {
                found.add(PsiReferenceBase.Immediate(element, true, property))
              }
            }
          }
        }
      }
      if (!found.isEmpty()) {
        return found.toTypedArray()
      }
      return PsiReference.EMPTY_ARRAY
    }

    if (expression is ILVariable) {
      if (name in TILCompletionProvider.SCOPES) return PsiReference.EMPTY_ARRAY
      val module = host.getTerraformModule()
      // TODO: get suitable resource/provider/etc
      // val model = ServiceManager.getService(TypeModelProvider::class.java).get()
      val resources = module.findResources(expression.name, name)
      if (resources.size == 0) return PsiReference.EMPTY_ARRAY
      return resources.map { HCLBlockNameReference(element, true, it, 2) }.toTypedArray()
      // TODO: support 'module.MODULE_NAME.OUTPUT_NAME' references (in that or another provider)
    }

    return PsiReference.EMPTY_ARRAY;
  }

  private fun getGoodLeftElement(select: ILSelectExpression, right: ILVariable): ILExpression? {
    // select = left.right
    val left = select.from
    if (left is ILSelectExpression) {
      // left = from.middle
      val middle = left.field
      val from = left.from
      if (middle?.text == "*" && from is ILSelectExpression) {
        // left == from.*
        // from == X.Y
        // select = X.Y.*.right
        // Y == from.field
        return from.field
      }
      return middle
    }

    if (left !== right) return left
    // TODO: Investigate is that enough
    return null
  }


}

