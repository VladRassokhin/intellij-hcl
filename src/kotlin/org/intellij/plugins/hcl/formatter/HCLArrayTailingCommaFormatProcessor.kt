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
package org.intellij.plugins.hcl.formatter

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.impl.source.codeStyle.PreFormatProcessor
import org.intellij.plugins.hcl.HCLLanguage


class HCLArrayTailingCommaFormatProcessor : PreFormatProcessor {
  override fun process(element: ASTNode, range: TextRange): TextRange {
    val psiElement = element.psi
    if (psiElement == null || !psiElement.isValid) return range
    if (!psiElement.language.isKindOf(HCLLanguage)) return range

    return try {
      ArrayTailingCommaFormatter(psiElement, range).process()
    } catch (e: PsiInvalidElementAccessException) {
      return range
    }
  }
}