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
package org.intellij.plugins.hcl.editor

import com.intellij.lang.Commenter
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.intellij.plugins.hcl.formatter.HCLCodeStyleSettings
import kotlin.properties.Delegates


class HCLCommenter : Commenter {

  private var customSettings : HCLCodeStyleSettings by Delegates.notNull()

  init {
    customSettings = CodeStyleSettingsManager.getInstance().
        currentSettings.getCustomSettings(HCLCodeStyleSettings::class.java)
  }

  override fun getLineCommentPrefix(): String? {
    return when (HCLCodeStyleSettings.LineCommenterCharacter.values()[customSettings.PROPERTY_LINE_COMMENTER_CHARACTER]){
      HCLCodeStyleSettings.LineCommenterCharacter.LINE_DOUBLE_SLASHES -> "//"
      HCLCodeStyleSettings.LineCommenterCharacter.LINE_POUND_SIGN -> "#"
    }
  }

  override fun getBlockCommentPrefix(): String? {
    return "/*"
  }

  override fun getBlockCommentSuffix(): String? {
    return "*/"
  }

  override fun getCommentedBlockCommentPrefix(): String? {
    return null
  }

  override fun getCommentedBlockCommentSuffix(): String? {
    return null
  }
}
