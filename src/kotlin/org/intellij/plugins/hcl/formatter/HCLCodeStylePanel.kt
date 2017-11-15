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
package org.intellij.plugins.hcl.formatter

import com.intellij.application.options.codeStyle.OptionTableWithPreviewPanel
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

/**
 * @author Vladislav Rassokhin
 */
class HCLCodeStylePanel(private val language: Language, settings: CodeStyleSettings) : OptionTableWithPreviewPanel(settings) {

  init {
    init()
  }

  override fun initTables() {
    // Format alignment
    val alignment_values = HCLCodeStyleSettings.PropertyAlignment.values()
    val alignment_strings = arrayOfNulls<String>(alignment_values.size)
    val alignment_ints = IntArray(alignment_values.size)
    for (i in alignment_values.indices) {
      alignment_strings[i] = alignment_values[i].description
      alignment_ints[i] = alignment_values[i].id
    }

    // Line Commenter character
    val linecommenterchar_values = HCLCodeStyleSettings.LineCommenterCharacter.values()
    val linecommenterchar_strings = arrayOfNulls<String>(linecommenterchar_values.size)
    val linecommenterchar_ints = IntArray(linecommenterchar_values.size)
    for (i in linecommenterchar_values.indices) {
      linecommenterchar_strings[i] = linecommenterchar_values[i].description
      linecommenterchar_ints[i] = linecommenterchar_values[i].id
    }

    showCustomOption(HCLCodeStyleSettings::class.java, "PROPERTY_ALIGNMENT", "Align properties", "Formatting options", alignment_strings, alignment_ints)
    showCustomOption(HCLCodeStyleSettings::class.java, "PROPERTY_LINE_COMMENTER_CHARACTER", "Line Commenter Character", "Code conventions", linecommenterchar_strings, linecommenterchar_ints)
  }

  override fun getSettingsType(): LanguageCodeStyleSettingsProvider.SettingsType {
    return LanguageCodeStyleSettingsProvider.SettingsType.LANGUAGE_SPECIFIC
  }

  override fun getDefaultLanguage(): Language? = language

  override fun getPreviewText(): String? = ALIGNMENT_SAMPLE

  companion object {
    val ALIGNMENT_SAMPLE = "simple = true\n" +
        "pa.int = false\n" +
        "under_score = 1\n" +
        "mi-nus = 'yep'\n" +
        "_5 = true\n" +
        "w1th.num8er5 = 'acceptable'"
  }
}
