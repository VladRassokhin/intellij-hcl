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

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object ILFileType: LanguageFileType(TILLanguage) {
  override fun getIcon(): Icon? {
    return AllIcons.FileTypes.Custom
  }

  override fun getDefaultExtension(): String {
    return "terraform.il"
  }

  override fun getDescription(): String {
    return "Interpolation Language in Terraform configs"
  }

  override fun getName(): String {
    return "Terraform-IL"
  }
}