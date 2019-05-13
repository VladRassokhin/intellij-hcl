/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.config.refactoring

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.HCLElementTypes.IDENTIFIER
import org.intellij.plugins.hcl.HCLElementTypes.STRING_LITERAL
import org.intellij.plugins.hcl.psi.HCLPsiUtil
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns
import java.util.regex.Pattern


class TerraformElementRenameValidator : RenameInputValidator {
  companion object {
    // TF: regexp.MustCompile(`(?i)\A[A-Z0-9_][A-Z0-9\-\_]*\z`)
    private val NamePattern = Pattern.compile("(?i)\\A[A-Z0-9_][A-Z0-9\\-_]*\\z", Pattern.CASE_INSENSITIVE)

    private fun isInputValid(name: String, isStringLiteral: Boolean): Boolean {
      // TF: "resource name can only contain letters, numbers, dashes, and underscores"
      @Suppress("NAME_SHADOWING")
      var name: String = name
      if (isStringLiteral) {
        if (!name.startsWith('\'') && !name.startsWith('\"')) {
          name = "\"" + name
        }
        if (!name.endsWith('\'') && !name.endsWith('\"')) {
          name += "\""
        }
      }
      return NamePattern.matcher(HCLPsiUtil.stripQuotes(name)).matches()
    }
  }

  override fun getPattern(): ElementPattern<out PsiElement> {
    return psiElement()
        .withElementType(TokenSet.create(STRING_LITERAL, IDENTIFIER))
        .withParent(or(TerraformPatterns.ResourceRootBlock, TerraformPatterns.DataSourceRootBlock))
        .with(TerraformPatterns.IsBlockNameIdentifier)
  }

  override fun isInputValid(name: String, element: PsiElement, context: ProcessingContext): Boolean {
    if (!pattern.accepts(element)) return false

    return isInputValid(name, element is HCLStringLiteral)
  }
}
