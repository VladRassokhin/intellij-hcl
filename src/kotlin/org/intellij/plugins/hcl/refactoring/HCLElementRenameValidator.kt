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
package org.intellij.plugins.hcl.refactoring

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.HCLLexer
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.HCLIdentifier
import org.intellij.plugins.hcl.psi.HCLStringLiteral


class HCLElementRenameValidator : RenameInputValidator {
  override fun getPattern(): ElementPattern<out PsiElement> {
    return or(
        psiElement(HCLStringLiteral::class.java),
        psiElement(HCLIdentifier::class.java)
    )
  }

  private val lexer = HCLLexer()

  override fun isInputValid(name: String?, element: PsiElement?, context: ProcessingContext?): Boolean {
    if (name == null || element == null) return false
    if (!pattern.accepts(element)) return false
    @Suppress("NAME_SHADOWING")
    var name: String = name
    if (element is HCLStringLiteral) {
      if (!name.startsWith('\'') && !name.startsWith('\"')) {
        name = "\"" + name
      }
      if (!name.endsWith('\'') && !name.endsWith('\"')) {
        name += "\""
      }
    }
    synchronized(lexer) {
      lexer.start(name)
      return lexer.tokenEnd == name.length && HCLParserDefinition.IDENTIFYING_LITERALS.contains(lexer.tokenType)
    }
  }
}
