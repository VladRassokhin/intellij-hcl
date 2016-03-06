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
package org.intellij.plugins.hcl.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.intellij.plugins.hcl.HCLLexer
import org.intellij.plugins.hcl.psi.*

open class HCLFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner(): WordsScanner? {
    return HCLWordsScanner(HCLLexer())
  }

  override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
    if (psiElement !is PsiNamedElement || psiElement !is HCLElement) {
      return false
    }
    if (psiElement is HCLStringLiteral || psiElement is HCLIdentifier) {
      @Suppress("USELESS_CAST")
      val parent = (psiElement as PsiElement).parent
      if (parent is HCLBlock) {
        return psiElement === parent.nameIdentifier
      } else if(parent is HCLProperty) {
        return psiElement === parent.nameIdentifier
      }
    }
    return true
  }

  override fun getHelpId(psiElement: PsiElement): String? {
    return null
  }

  override fun getType(element: PsiElement): String {
    if (element is HCLBlock) {
      return "Block('${element.nameElements.first()!!.text}')"
    }
    if (element is HCLProperty) {
      return "Property"
    }
    if (element is HCLIdentifier || element is HCLStringLiteral) {
      val parent = element.parent
      if (parent is HCLBlock) {
        return parent.nameElements.first()!!.text
      }
      if (parent is HCLProperty && parent.nameElement === element) {
        return "Property"
      }
    }

    if (element is PsiNamedElement) {
      //      return element.name ?:
      return "<Untyped PsiNamedElement${element.javaClass.name}>"
    }
    return "<Untyped non-PsiNamedElement ${element.node.elementType}>"
  }

  override fun getDescriptiveName(element: PsiElement): String {
    val name = if (element is PsiNamedElement) element.name else null
    return name ?: "<Not An PsiNamedElement ${element.node.elementType}>"
  }

  override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
    if (useFullName) {
      if (element is HCLBlock) {
        return element.fullName
      }
    }
    return getDescriptiveName(element)
  }
}
