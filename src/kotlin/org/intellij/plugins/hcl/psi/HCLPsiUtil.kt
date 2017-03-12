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
package org.intellij.plugins.hcl.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement

/**
 * Various helper methods for working with PSI of JSON language.

 * @author Mikhail Golubev
 */
object HCLPsiUtil {
  /**
   * Checks that PSI element represents key of JSON property (key-value pair of JSON object)

   * @param element PSI element to check
   * *
   * @return whether this PSI element is property key
   */
  fun isPropertyKey(element: PsiElement): Boolean {
    val parent = element.parent
    return parent is HCLProperty && element === parent.nameElement
  }

  /**
   * Checks that PSI element represents value of JSON property (key-value pair of JSON object)

   * @param element PSI element to check
   * *
   * @return whether this PSI element is property value
   */
  fun isPropertyValue(element: PsiElement): Boolean {
    val parent = element.parent
    return parent is HCLProperty && element === parent.value
  }

  /**
   * Returns text of the given PSI element. Unlike obvious [PsiElement.getText] this method unescapes text of the element if latter
   * belongs to injected code fragment using [InjectedLanguageManager.getUnescapedText].

   * @param element PSI element which text is needed
   * *
   * @return text of the element with any host escaping removed
   */
  fun getElementTextWithoutHostEscaping(element: PsiElement): String {
    val manager = InjectedLanguageManager.getInstance(element.project)
    if (manager.isInjectedFragment(element.containingFile)) {
      return manager.getUnescapedText(element)
    } else {
      return element.text
    }
  }

  /**
   * Returns content of the string literal (without escaping) striving to preserve as much of user data as possible.
   *
   *  * If literal length is greater than one and it starts and ends with the same quote and the last quote is not escaped, returns
   * text without first and last characters.
   *  * Otherwise if literal still begins with a quote, returns text without first character only.
   *  * Returns unmodified text in all other cases.
   *

   * @param text presumably result of [HCLStringLiteral.getText]
   * *
   * @return
   */
  fun stripQuotes(text: String): String {
    if (text.isNotEmpty()) {
      val firstChar = text[0]
      val lastChar = text[text.length - 1]
      if (firstChar == '\'' || firstChar == '"') {
        if (text.length > 1 && firstChar == lastChar && !isEscapedChar(text, text.length - 1)) {
          return text.substring(1, text.length - 1)
        }
        return text.substring(1)
      }
    }
    return text
  }

  /**
   * Checks that character in given position is escaped with backslashes.

   * @param text     text character belongs to
   * *
   * @param position position of the character
   * *
   * @return whether character at given position is escaped, i.e. preceded by odd number of backslashes
   */
  fun isEscapedChar(text: String, position: Int): Boolean {
    var count = 0
    var i = position - 1
    while (i >= 0 && text[i] == '\\') {
      count++
      i--
    }
    return count % 2 != 0
  }

}// empty
