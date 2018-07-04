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

import java.util.*

// Model for element types

open class Type(val name: String) {
  override fun toString(): String {
    return "Type{$name}"
  }
}

open class BaseModelType(val description: String? = null,
                         val required: Boolean = false,
                         val deprecated: String? = null,
                         val computed: Boolean = false,
                         val conflictsWith: List<String> = emptyList()
)

interface Hint
open class SimpleHint(vararg val hint: String) : Hint
open class TypeHint(val hint: Type) : Hint
open class ListHint(val hint: List<PropertyOrBlockType>) : Hint

// TODO: Use some 'Reference' class
open class ReferenceHint(vararg val hint: String) : Hint

// TODO: Use Interpolation result type
open class InterpolationHint(hint: String) : SimpleHint(hint)

open class SimpleValueHint(vararg hint: String) : SimpleHint(*hint)

// TODO: Support 'default' values for certain types
open class PropertyType(val name: String, val type: Type,
                        val hint: Hint? = null,
                        val injectionAllowed: Boolean = true,
                        description: String? = null,
                        required: Boolean = false, deprecated: String? = null, computed: Boolean = false,
                        conflictsWith: List<String> = emptyList(),
                        val has_default: Boolean = false
) : BaseModelType(description = description, required = required, deprecated = deprecated, computed = computed, conflictsWith = conflictsWith) {
  override fun toString(): String {
    return "PropertyType(name='$name', type='$type')"
  }
}

open class BlockType(val literal: String, val args: Int = 0,
                     description: String? = null,
                     required: Boolean = false, deprecated: String? = null, computed: Boolean = false,
                     conflictsWith: List<String> = emptyList(),
                     vararg val properties: PropertyOrBlockType = arrayOf()
) : BaseModelType(description = description, required = required, deprecated = deprecated, computed = computed, conflictsWith = conflictsWith) {
  override fun toString(): String {
    return "BlockType(literal='$literal', args=$args, properties=${Arrays.toString(properties)})"
  }
}

class PropertyOrBlockType private constructor(val property: PropertyType? = null, val block: BlockType? = null) {
  val name: String get() = property?.name ?: block!!.literal
  val required: Boolean get() = property?.required ?: block!!.required
  val deprecated: String? get() = if (property != null) property.deprecated else block!!.deprecated
  val computed: Boolean get() = property?.computed ?: block!!.computed

  init {
    assert(property != null || block != null) { "Either property or block expected" }
  }

  constructor(property: PropertyType) : this(property, null)

  constructor(block: BlockType) : this(null, block)

  override fun toString(): String {
    if (property != null)
      return "POBT-Property(name='$name', required=$required, deprecated=$deprecated)"
    else
      return "POBT-Block(name='$name', required=$required, deprecated=$deprecated)"
  }
}

fun PropertyType.toPOBT(): PropertyOrBlockType {
  return PropertyOrBlockType(this)
}

fun BlockType.toPOBT(): PropertyOrBlockType {
  return PropertyOrBlockType(this)
}

object Types {
  val Identifier = Type("Identifier")
  val String = Type("String")
  val Number = Type("Number")
  val Boolean = Type("Boolean")
  val Null = Type("Null")
  val Array = Type("Array")
  val Object = Type("Object")
  val Invalid = Type("Invalid")

  val Any = Type("Any") // From interpolation

  // Separate, as could be used as String, Number, Boolean, etc
  val StringWithInjection = Type("String")

  val SimpleValueTypes = setOf(Types.String, Types.Number, Types.Boolean)
}

class ResourceType(val type: String, val provider: ProviderType, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("resource", 2, properties = *properties) {
  override fun toString(): String {
    return "ResourceType(type='$type', provider=${provider.type}) ${super.toString()}"
  }
}

class DataSourceType(val type: String, val provider: ProviderType, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("data", 2, properties = *properties) {
  override fun toString(): String {
    return "DataSourceType(type='$type', provider=${provider.type}) ${super.toString()}"
  }
}

class ProviderType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("provider", 1, properties = *properties) {
  override fun toString(): String {
    return "ProviderType(type='$type') ${super.toString()}"
  }
}

class ProvisionerType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("provisioner", 1, properties = *properties) {
  override fun toString(): String {
    return "ProvisionerType(type='$type') ${super.toString()}"
  }
}

class BackendType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("backend", 1, properties = *properties) {
  override fun toString(): String {
    return "BackendType(type='$type') ${super.toString()}"
  }
}
