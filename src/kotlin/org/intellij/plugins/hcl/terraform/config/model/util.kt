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
package org.intellij.plugins.hcl.terraform.config.model

import org.intellij.plugins.hcl.psi.*

class ModelUtil private constructor() {
  companion object {
    fun getValueType(value: HCLValue?): Type? {
      if (value == null) return null
      return when (value) {
        is HCLObject -> Types.Object
        is HCLArray -> Types.Array
        is HCLIdentifier -> Types.Identifier
        is HCLStringLiteral -> Types.String
        is HCLHeredocLiteral -> Types.String
        is HCLNumberLiteral -> Types.Number
        is HCLBooleanLiteral -> Types.Boolean
        is HCLNullLiteral -> Types.Null
        else -> null
      }
    }
  }
}

fun String.ensureHavePrefix(prefix: String) = if (this.startsWith(prefix)) this else (prefix + this)
