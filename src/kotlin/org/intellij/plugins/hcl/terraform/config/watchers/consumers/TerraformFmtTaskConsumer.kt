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
package org.intellij.plugins.hcl.terraform.config.watchers.consumers

import com.intellij.ide.macro.FilePathMacro
import com.intellij.plugins.watcher.model.TaskOptions
import org.intellij.plugins.hcl.terraform.TerraformToolProjectSettings
import org.intellij.plugins.hcl.terraform.config.watchers.macros.TerraformExecutableMacro

class TerraformFmtTaskConsumer : TerraformToolTaskConsumer() {
  override fun getOptionsTemplate(): TaskOptions {
    val options = createDefaultOptions()
    options.name = "terraform fmt"
    options.description = "Runs `terraform fmt` on Terraform HCL config file"
    options.program = if (TerraformExecutableMacro.isRegistered()) {
      "$" + TerraformExecutableMacro().name + "$"
    } else {
      TerraformToolProjectSettings.getDefaultTerraformPath()
    }
    options.arguments = "fmt $" + FilePathMacro().name + "$"
    options.output = "$" + FilePathMacro().name + "$"
    return options
  }
}