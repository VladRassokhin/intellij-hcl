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
package org.intellij.plugins.hil.refactoring

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.intellij.plugins.hil.HILLanguage
import org.intellij.plugins.hil.psi.ILExpression

object ILCodeInsightUtil {
  fun <T : PsiElement> findElementInRange(file: PsiFile, startOffset: Int, endOffset: Int, klass: Class<T>): T {
    return CodeInsightUtilCore.findElementInRange(file, startOffset, endOffset, klass, HILLanguage)
  }

  fun findExpressionInRange(file: PsiFile, startOffset: Int, endOffset: Int): ILExpression? {
    if (!file.viewProvider.languages.contains(HILLanguage)) return null
    return findElementInRange(file, startOffset, endOffset, ILExpression::class.java)
  }
}