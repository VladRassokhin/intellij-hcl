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

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.intellij.plugins.hcl.HCLLanguage

public class HCLCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
  override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable {
    return object : CodeStyleAbstractConfigurable(settings, originalSettings, "HCL") {
      override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
        val language = HCLLanguage
        val currentSettings = getCurrentSettings()
        return object : TabbedLanguageCodeStylePanel(language, currentSettings, settings) {
          override fun initTabs(settings: CodeStyleSettings) {
            addIndentOptionsTab(settings)
            addSpacesTab(settings)
            addBlankLinesTab(settings)
            addWrappingAndBracesTab(settings)
            addTab(HCLCodeStylePanel(settings))
          }
        }
      }

      override fun getHelpTopic(): String? {
        return null
      }
    }
  }

  override fun getConfigurableDisplayName(): String? {
    return HCLLanguage.getDisplayName()
  }

  override fun createCustomSettings(settings: CodeStyleSettings?): CustomCodeStyleSettings? {
    return HCLCodeStyleSettings(settings!!)
  }
}
