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
                         val conflictsWith: List<String>? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BaseModelType) return false

    if (description != other.description) return false
    if (required != other.required) return false
    if (deprecated != other.deprecated) return false
    if (computed != other.computed) return false
    if (conflictsWith != other.conflictsWith) return false

    return true
  }

  override fun hashCode(): Int {
    var result = description?.hashCode() ?: 0
    result = 31 * result + required.hashCode()
    result = 31 * result + (deprecated?.hashCode() ?: 0)
    result = 31 * result + computed.hashCode()
    result = 31 * result + (conflictsWith?.hashCode() ?: 0)
    return result
  }
}

interface Hint
open class SimpleHint(vararg val hint: String) : Hint
data class TypeHint(val hint: Type) : Hint
data class ListHint(val hint: List<PropertyOrBlockType>) : Hint

// TODO: Use some 'Reference' class
open class ReferenceHint(vararg val hint: String) : Hint

// TODO: Use Interpolation result type
open class InterpolationHint(hint: String) : SimpleHint(hint)

open class SimpleValueHint(vararg hint: String) : SimpleHint(*hint)

// TODO: Support 'default' values for certain types
open class PropertyType(override val name: String, val type: Type,
                        val hint: Hint? = null,
                        val injectionAllowed: Boolean = true,
                        description: String? = null,
                        required: Boolean = false, deprecated: String? = null, computed: Boolean = false,
                        conflictsWith: List<String>? = null,
                        override val defaultValue: Any? = null
) : BaseModelType(description = description, required = required, deprecated = deprecated, computed = computed, conflictsWith = conflictsWith), PropertyOrBlockType {

  override fun toString(): String {
    return "PropertyType(name='$name', type='$type')"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as PropertyType

    if (name != other.name) return false
    if (type != other.type) return false
    if (hint != other.hint) return false
    if (injectionAllowed != other.injectionAllowed) return false
    if (defaultValue != other.defaultValue) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + (hint?.hashCode() ?: 0)
    result = 31 * result + injectionAllowed.hashCode()
    result = 31 * result + (defaultValue?.hashCode() ?: 0)
    return result
  }

}

open class BlockType(val literal: String, val args: Int = 0,
                     description: String? = null,
                     required: Boolean = false, deprecated: String? = null, computed: Boolean = false,
                     conflictsWith: List<String>? = null,
                     vararg properties: PropertyOrBlockType = emptyArray()
) : BaseModelType(description = description, required = required, deprecated = deprecated, computed = computed, conflictsWith = conflictsWith), PropertyOrBlockType {
  override val name: String
    get() = literal

  val properties: Array<out PropertyOrBlockType> = if (properties.isEmpty()) PropertyOrBlockType.EMPTY_ARRAY else properties

  override fun toString(): String {
    return "BlockType(literal='$literal', args=$args, properties=${Arrays.toString(properties)})"
  }

  override val defaultValue: Any?
    get() = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as BlockType

    if (literal != other.literal) return false
    if (args != other.args) return false
    if (!properties.contentEquals(other.properties)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + literal.hashCode()
    result = 31 * result + args
    result = 31 * result + properties.contentHashCode()
    return result
  }

}

interface PropertyOrBlockType {
  companion object {
    val EMPTY_ARRAY: Array<out PropertyOrBlockType> = emptyArray()
  }

  val name: String
  val required: Boolean
  val deprecated: String?
  val computed: Boolean
  val conflictsWith: List<String>?
  val defaultValue: Any?
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

class ResourceType(val type: String, val provider: ProviderType, vararg properties: PropertyOrBlockType) : BlockType("resource", 2, properties = *properties) {
  override fun toString(): String {
    return "ResourceType(type='$type', provider=${provider.type}) ${super.toString()}"
  }
}

class DataSourceType(val type: String, val provider: ProviderType, vararg properties: PropertyOrBlockType) : BlockType("data", 2, properties = *properties) {
  override fun toString(): String {
    return "DataSourceType(type='$type', provider=${provider.type}) ${super.toString()}"
  }
}

class ProviderType(val type: String, vararg properties: PropertyOrBlockType) : BlockType("provider", 1, properties = *properties) {
  override fun toString(): String {
    return "ProviderType(type='$type') ${super.toString()}"
  }
}

class ProvisionerType(val type: String, vararg properties: PropertyOrBlockType) : BlockType("provisioner", 1, properties = *properties) {
  override fun toString(): String {
    return "ProvisionerType(type='$type') ${super.toString()}"
  }
}

class BackendType(val type: String, vararg properties: PropertyOrBlockType) : BlockType("backend", 1, properties = *properties) {
  override fun toString(): String {
    return "BackendType(type='$type') ${super.toString()}"
  }
}
