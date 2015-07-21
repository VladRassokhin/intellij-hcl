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
package org.intellij.plugins.hcl.formatter

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.intellij.plugins.hcl.HCLLanguage

public open class HCLLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
  override fun getLanguage(): Language = HCLLanguage

  override fun getCodeSample(settingsType: LanguageCodeStyleSettingsProvider.SettingsType): String {
    // TODO: Improve sample
    return "name = value\n" +
        "resource 'a' {\n" +
        "  \"x\" = [1e-9,]\n" +
        "}"
  }

  override fun getIndentOptionsEditor(): IndentOptionsEditor? = SmartIndentOptionsEditor()

  override fun getDefaultCommonSettings(): CommonCodeStyleSettings? {
    val commonSettings = CommonCodeStyleSettings(HCLLanguage)
    val indentOptions = commonSettings.initIndentOptions()
    indentOptions.INDENT_SIZE = 2
    return commonSettings
  }

  override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: LanguageCodeStyleSettingsProvider.SettingsType) {
    when (settingsType) {
      LanguageCodeStyleSettingsProvider.SettingsType.SPACING_SETTINGS -> {
        consumer.showStandardOptions("SPACE_WITHIN_BRACKETS", "SPACE_WITHIN_BRACES", "SPACE_AFTER_COMMA", "SPACE_BEFORE_COMMA", "SPACE_AROUND_ASSIGNMENT_OPERATORS")
        consumer.renameStandardOption("SPACE_WITHIN_BRACES", "Braces")
        consumer.renameStandardOption("SPACE_AROUND_ASSIGNMENT_OPERATORS", "Equals")
      }
      LanguageCodeStyleSettingsProvider.SettingsType.BLANK_LINES_SETTINGS -> {
        consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE")
      }
      LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
        consumer.showStandardOptions("RIGHT_MARGIN", "KEEP_LINE_BREAKS", "WRAP_LONG_LINES")
        consumer.showCustomOption(javaClass<HCLCodeStyleSettings>(), "ARRAY_WRAPPING", "Arrays", null, CodeStyleSettingsCustomizable.WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES)
        consumer.showCustomOption(javaClass<HCLCodeStyleSettings>(), "OBJECT_WRAPPING", "Objects", null, CodeStyleSettingsCustomizable.WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES)
      }
    }
  }
}
