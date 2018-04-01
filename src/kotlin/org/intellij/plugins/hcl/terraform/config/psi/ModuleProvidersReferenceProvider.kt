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
package org.intellij.plugins.hcl.terraform.config.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.model.Module
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hil.psi.HCLElementLazyReferenceBase

object ModuleProvidersReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    // ModuleBlock -> Object -> (Block|Property) -> Object -> Property -> element (either key or value)
    if (element !is HCLElement) return PsiReference.EMPTY_ARRAY
    if (element !is HCLIdentifier && element !is HCLStringLiteral) return PsiReference.EMPTY_ARRAY
    val property = element.getParent(HCLProperty::class.java) ?: return PsiReference.EMPTY_ARRAY

    val block = property.getParent(HCLObject::class.java).getParent(HCLObject::class.java).getParent(HCLBlock::class.java)
        ?: return PsiReference.EMPTY_ARRAY

    val type = block.getNameElementUnquoted(0) ?: return PsiReference.EMPTY_ARRAY
    if (type != "module") return PsiReference.EMPTY_ARRAY

    return arrayOf(Reference(element))
  }

  class Reference(element: HCLElement) : HCLElementLazyReferenceBase<HCLElement>(element, false) {
    override fun resolve(incompleteCode: Boolean, includeFake: Boolean): List<HCLElement> {
      val element = this.element!!
      val property = element.getParent(HCLProperty::class.java) ?: return emptyList()
      val block = property.getParent(HCLObject::class.java).getParent(HCLObject::class.java).getParent(HCLBlock::class.java)
          ?: return emptyList()
      val type = block.getNameElementUnquoted(0) ?: return emptyList()
      if (type != "module") return emptyList()
      val value = getValue(element) ?: return emptyList()

      val module: Module =
          if (HCLPsiUtil.isPropertyKey(element)) {
            // Key, search referenced module
            Module.getAsModuleBlock(block) ?: return emptyList()
          } else {
            // Value, search current module
            element.getTerraformModule()
          }
      return if (incompleteCode) {
        module.getDefinedProviders().map { it.first.nameIdentifier as HCLElement }
      } else {
        module.findProviders(value).map { it.nameIdentifier as HCLElement }
      }
    }

    companion object {
      private fun getValue(element: HCLElement): String? {
        if (element is HCLIdentifier) return element.id
        if (element is HCLStringLiteral) return element.value
        return null
      }
    }
  }
}