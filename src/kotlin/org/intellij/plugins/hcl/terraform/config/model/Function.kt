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
package org.intellij.plugins.hcl.terraform.config.model

open class Argument(val type: Type, val name: String? = null)
open class VariadicArgument(type: Type, name: String? = null) : Argument(type, name)

class Function(val name: String, val ret: Type, vararg val arguments: Argument = emptyArray(), val variadic: VariadicArgument? = null) {
  init {
    val count = arguments.count { it is VariadicArgument }
    assert (count == 0 || (count == 1 && arguments.last() is VariadicArgument)) { "Only one (last) argument could be variadic" }
  }
}