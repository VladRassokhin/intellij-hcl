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
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.BuildNumber

class MacrosInstaller : ApplicationComponent.Adapter() {
  companion object {
    val isPluginMacrosSupported by lazy {ApplicationInfoEx.getInstanceEx().build >= BuildNumber.fromString("173.2100")}
  }

  override fun initComponent() {
    if (isPluginMacrosSupported) {
      val point = Extensions.getArea(null).getExtensionPoint<Macro>(Macro.EP_NAME)
      point.registerExtension(TerraformExecutableMacro())
    }
  }
}