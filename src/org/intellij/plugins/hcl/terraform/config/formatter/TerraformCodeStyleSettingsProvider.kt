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
package org.intellij.plugins.hcl.terraform.config.formatter

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.intellij.plugins.hcl.formatter.HCLCodeStylePanel
import org.intellij.plugins.hcl.formatter.HCLCodeStyleSettings
import org.intellij.plugins.hcl.formatter.HCLCodeStyleSettingsProvider
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage

public class TerraformCodeStyleSettingsProvider : HCLCodeStyleSettingsProvider(TerraformLanguage) {
  override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable {
    return object : CodeStyleAbstractConfigurable(settings, originalSettings, "Terraform") {
      override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
        val currentSettings = getCurrentSettings()
        return object : TabbedLanguageCodeStylePanel(_language, currentSettings, settings) {
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
}

