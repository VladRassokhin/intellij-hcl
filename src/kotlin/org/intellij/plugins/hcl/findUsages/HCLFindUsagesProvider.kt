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
package org.intellij.plugins.hcl.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.intellij.plugins.hcl.HCLLexer
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLProperty

open class HCLFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner(): WordsScanner? {
    return HCLWordsScanner(HCLLexer())
  }

  override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
    return psiElement is PsiNamedElement
  }

  override fun getHelpId(psiElement: PsiElement): String? {
    return null
  }

  override fun getType(element: PsiElement): String {
    if (element is HCLBlock) {
      return "hcl.block"
    }
    if (element is HCLProperty) {
      return "hcl.property"
    }
    return ""
  }

  override fun getDescriptiveName(element: PsiElement): String {
    val name = if (element is PsiNamedElement) element.name else null
    return name ?: "<unnamed>"
  }

  override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
    return getDescriptiveName(element)
  }
}
