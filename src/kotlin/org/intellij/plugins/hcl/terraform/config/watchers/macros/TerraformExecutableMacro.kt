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
package org.intellij.plugins.hcl.terraform.config.watchers.macros

import com.intellij.ide.macro.Macro
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import org.intellij.plugins.hcl.terraform.TerraformToolProjectSettings

class TerraformExecutableMacro : Macro() {
  override fun getName(): String {
    return "TerraformExecPath"
  }

  override fun getDescription(): String {
    return "Terraform executable path"
  }

  @Throws(Macro.ExecutionCancelledException::class)
  override fun expand(dataContext: DataContext): String? {
    val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return null
    return TerraformToolProjectSettings.getInstance(project).terraformPath
  }
}
