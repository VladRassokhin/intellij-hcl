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
import org.intellij.plugins.hcl.psi.HCLHeredocLiteral
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.il.HILElementTypes.INTERPOLATION_END
import org.intellij.plugins.hcl.terraform.il.HILElementTypes.INTERPOLATION_START
import org.intellij.plugins.hcl.terraform.il.psi.HILLexer
import java.util.*

public class ILLanguageInjector : LanguageInjector {
  override fun getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces) {
    if (host !is HCLStringLiteral && host !is HCLHeredocLiteral) return;
    // Only .tf (Terraform config) files
    val file = host.containingFile ?: return
    if (file.fileType !is TerraformFileType) return;
    if (host is HCLStringLiteral) return getStringLiteralInjections(host, places);
    if (host is HCLHeredocLiteral) return getHeredocLiteralInjections(host, places);
    return;
  }

  private fun getStringLiteralInjections(host: HCLStringLiteral, places: InjectedLanguagePlaces) {
    val text = host.text
    val value = host.value
    val offset = if (value != text) 1 else 0
    val ranges = getILRangesInText(value)
    for (range in ranges) {
      val rng = range.shiftRight(offset)
      places.addPlace(HILLanguage, rng, null, null)
    }
  }

  private fun getHeredocLiteralInjections(host: HCLHeredocLiteral, places: InjectedLanguagePlaces) {
    val lines = host.linesList
    if (lines.isEmpty()) return
    for (line in lines) {
      val ranges = getILRangesInText(line.value)
      if (ranges.isEmpty()) continue
      val offset = line.startOffsetInParent
      for (range in ranges) {
        val rng = range.shiftRight(offset)
        places.addPlace(HILLanguage, rng, null, null)
      }
    }
  }

  companion object {
    public fun getILRangesInText(text: String): ArrayList<TextRange> {
      if (!text.contains("${"$"}{")) return arrayListOf();

      val ranges: ArrayList<TextRange> = ArrayList()
      var skip = text.indexOf("\${");
      out@ while (true) {
        if (skip >= text.length) break;

        val lexer = HILLexer()
        lexer.start(text, skip, text.length);
        var level = 0
        var start = -1;
        while (true) {
          val type = lexer.tokenType
          when (type) {
            INTERPOLATION_START -> {
              if (level == 0) {
                start = lexer.tokenStart
              }
              level++;
            }
            INTERPOLATION_END -> {
              if (level <= 0) {
                // Incorrect state, probably just '}' in text retry from current position.
                skip = lexer.tokenStart + 1;
                continue@out;
              }
              level--;
              if (level == 0) {
                ranges.add(TextRange(start, lexer.tokenEnd));
                skip = lexer.tokenEnd;
                continue@out;
              }
            }
            null -> {
              if (lexer.tokenEnd >= text.length) {
                // Real end of string
                if (level > 0) {
                  // Non finished interpolation
                  ranges.add(TextRange(start, Math.min(lexer.tokenEnd, text.length)));
                }
                break@out;
              } else {
                // Non-parsable, probably not IL, retry from current position.
                skip = lexer.tokenStart + 1;
                continue@out;
              }
            }
            else -> {
              if (level == 0) {
                // Non-parsable, probably not IL, retry from current position.
                skip = lexer.tokenStart + 1;
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