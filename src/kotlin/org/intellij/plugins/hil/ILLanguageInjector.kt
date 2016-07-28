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
package org.intellij.plugins.hil

import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.hcl.psi.HCLFile
import org.intellij.plugins.hcl.psi.HCLHeredocContent
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hil.HILElementTypes.INTERPOLATION_END
import org.intellij.plugins.hil.HILElementTypes.INTERPOLATION_START
import org.intellij.plugins.hil.psi.HILLexer
import java.util.*

class ILLanguageInjector : LanguageInjector {
  override fun getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces) {
    if (host !is HCLStringLiteral && host !is HCLHeredocContent) return;
    // Only .tf (Terraform config) files
    val file = host.containingFile
    if (file !is HCLFile || !file.isInterpolationsAllowed()) return;
    // Restrict interpolations in .tfvars files // TODO: This file shouldn't know about .tfvars here
    if (file.fileType == TerraformFileType && file.name.endsWith("." + TerraformFileType.TFVARS_EXTENSION)) return
    if (host is HCLStringLiteral) return getStringLiteralInjections(host, places);
    if (host is HCLHeredocContent) return getHCLHeredocContentInjections(host, places);
    return;
  }

  private fun getStringLiteralInjections(host: HCLStringLiteral, places: InjectedLanguagePlaces) {
    if (!host.text.contains("\${")) return
    for (pair in host.textFragments) {
      val fragment = pair.second
      if (!fragment.startsWith("\${")) continue
      val ranges = getILRangesInText(fragment)
      for (range in ranges) {
        val rng = range.shiftRight(pair.first.startOffset)
        places.addPlace(HILLanguage, rng, null, null)
      }
    }
  }

  private fun getHCLHeredocContentInjections(host: HCLHeredocContent, places: InjectedLanguagePlaces) {
    if (host.linesCount == 0) return
    val lines = host.lines
    if (lines.isEmpty()) return
    var off:Int = 0
    for (line in lines) {
      val ranges = getILRangesInText(line)
      if (!ranges.isEmpty()) {
        val offset = off
        for (range in ranges) {
          val rng = range.shiftRight(offset)
          places.addPlace(HILLanguage, rng, null, null)
        }
      }
      off += line.length
    }
  }

  companion object {
    fun getILRangesInText(text: String): ArrayList<TextRange> {
      if (!text.contains("\${")) return arrayListOf();

      var skip = findInterpolationStart(text)
      if (skip == -1) return arrayListOf()

      val ranges: ArrayList<TextRange> = ArrayList()
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

    private fun findInterpolationStart(text: String): Int {
      var index: Int = -1
      do {
        index = text.indexOf("\${", index + 1)
      } while (index > 0 && text[index - 1] == '$')
      return index
    }

  }
}
