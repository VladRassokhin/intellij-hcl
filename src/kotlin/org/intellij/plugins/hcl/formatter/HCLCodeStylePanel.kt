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
    val alignmentValues = HCLCodeStyleSettings.PropertyAlignment.values()
    val alignmentStrings = arrayOfNulls<String>(alignmentValues.size)
    val alignmentInts = IntArray(alignmentValues.size)
    for (i in alignmentValues.indices) {
      alignmentStrings[i] = alignmentValues[i].description
      alignmentInts[i] = alignmentValues[i].id
    }

    // Line Commenter character
    val lineCommenterPrefixValues = HCLCodeStyleSettings.LineCommenterPrefix.values()
    val lineCommenterPrefixStrings = arrayOfNulls<String>(lineCommenterPrefixValues.size)
    val lineCommenterPrefixInts = IntArray(lineCommenterPrefixValues.size)
    for (i in lineCommenterPrefixValues.indices) {
      lineCommenterPrefixStrings[i] = lineCommenterPrefixValues[i].description
      lineCommenterPrefixInts[i] = lineCommenterPrefixValues[i].id
    }

    showCustomOption(HCLCodeStyleSettings::class.java, "PROPERTY_ALIGNMENT", "Align properties", "Formatting options", alignmentStrings, alignmentInts)
    showCustomOption(HCLCodeStyleSettings::class.java, "PROPERTY_LINE_COMMENTER_CHARACTER", "Line Commenter Character", "Code conventions", lineCommenterPrefixStrings, lineCommenterPrefixInts)
  }

  override fun getSettingsType(): LanguageCodeStyleSettingsProvider.SettingsType {
    return LanguageCodeStyleSettingsProvider.SettingsType.LANGUAGE_SPECIFIC
  }

  override fun getDefaultLanguage(): Language? = language

  override fun getPreviewText(): String? = ALIGNMENT_SAMPLE

  companion object {
    val ALIGNMENT_SAMPLE = "simple = true\n" +
        "pa.int = false\n" +
        "under_scr = 1\n" +
        "mi-nus = 'yep'\n\n" +
        "#another logical block\n" +
        "_5 = true\n" +
        "w1th.num8er5 = 'acceptable'"
  }
}
