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
package org.intellij.plugins.hcl.terraform.il

import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.hcl.psi.impl.HCLStringLiteralImpl
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.il.TILElementTypes.INTERPOLATION_END
import org.intellij.plugins.hcl.terraform.il.TILElementTypes.INTERPOLATION_START
import org.intellij.plugins.hcl.terraform.il.psi.TILLexer
import java.util.ArrayList

public class ILLanguageInjector : LanguageInjector {
  override fun getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces) {
    if (host !is HCLStringLiteralImpl) return;
    // Only .tf (Terraform config) files
    val file = host.getContainingFile() ?: return
    if (file.getFileType() !is TerraformFileType) return;
    val text = host.getText()
    val value = host.getValue()
    val ranges = getILRangesInText(value)
    for (range in ranges) {
      val rng = if (value != text) range.shiftRight(1) else range
      places.addPlace(TILLanguage, rng, null, null)
    }
  }

  companion object {
    public fun getILRangesInText(text: String): ArrayList<TextRange> {
      if (!text.contains("${"$"}{")) return arrayListOf();

      val ranges: ArrayList<TextRange> = ArrayList()
      var skip = text.indexOf("\${");
      out@ while (true) {
        if (skip >= text.length()) break;

        val lexer = TILLexer()
        lexer.start(text, skip, text.length());
        var level = 0
        var start = -1;
        while (true) {
          val type = lexer.getTokenType()
          when (type) {
            INTERPOLATION_START -> {
              if (level == 0) {
                start = lexer.getTokenStart()
              }
              level++;
            }
            INTERPOLATION_END -> {
              if (level <= 0) {
                // Incorrect state, probably just '}' in text retry from current position.
                skip = lexer.getTokenStart() + 1;
                continue@out;
              }
              level--;
              if (level == 0) {
                ranges.add(TextRange(start, lexer.getTokenEnd()));
                skip = lexer.getTokenEnd();
                continue@out;
              }
            }
            null -> {
              if (lexer.getTokenEnd() >= text.length()) {
                // Real end of string
                if (level > 0) {
                  // Non finished interpolation
                  ranges.add(TextRange(start, Math.min(lexer.getTokenEnd(), text.length())));
                }
                break@out;
              } else {
                // Non-parsable, probably not IL, retry from current position.
                skip = lexer.getTokenStart() + 1;
                continue@out;
              }
            }
            else -> {
              if (level == 0) {
                // Non-parsable, probably not IL, retry from current position.
                skip = lexer.getTokenStart() + 1;
                continue@out;
              }
            }
          }
          lexer.advance();
        }
      }
      return ranges;
    }

  }
}