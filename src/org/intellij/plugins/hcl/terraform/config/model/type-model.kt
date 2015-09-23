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

import com.beust.klaxon.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.w3c.dom.Document
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern

// Model for element types

public open class Type(val name: String)
public open class PropertyType(val name: String, val type: Type, val typeHint: String? = null, val description: String? = null, val required: Boolean = false)
public open class BlockType(val literal: String, val args: Int = 0, val required: Boolean = false, vararg val properties: PropertyOrBlockType = arrayOf())

public class PropertyOrBlockType private constructor(val property: PropertyType? = null, val block: BlockType? = null) {
  val name: String = if (property != null) property.name else block!!.literal
  val required: Boolean = if (property != null) property.required else block!!.required

  init {
    assert(property != null || block != null);
  }

  constructor(property: PropertyType) : this(property, null)

  constructor(block: BlockType) : this(null, block)
}

public fun PropertyType.toPOBT(): PropertyOrBlockType {
  return PropertyOrBlockType(this)
}

public fun BlockType.toPOBT(): PropertyOrBlockType {
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

  // Separate, as could be used as String, Number, Boolean, etc
  val StringWithInjection = Type("String")
}

public class ResourceType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("resource", 2, false, *properties)
public class ProviderType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("provider", 1, false, *properties)
public class VariableType(vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("variable", 1, false, *properties)

val DefaultResourceTypeProperties: Array<PropertyOrBlockType> = arrayOf(
    PropertyType("count", Types.Number).toPOBT(),
    PropertyType("depends_on", Types.Array, "String").toPOBT(),
    PropertyType("provider", Types.String, "(provider.type|provider.alias)").toPOBT(),
    BlockType("lifecycle", 0, false,
        PropertyType("create_before_destroy", Types.Boolean).toPOBT(),
        PropertyType("prevent_destroy", Types.Boolean).toPOBT()
    ).toPOBT()
    // Also may have connection? and provisioner+ blocks
)
val DefaultProviderTypeProperties: Array<PropertyOrBlockType> = arrayOf()


public class TypeModelProvider {
  val model: TypeModel by lazy { loadModel() }
  val external: Map<String, Additional> by lazy { loadExternalInformation() }

  public fun get(): TypeModel = model

  private fun loadModel(): TypeModel {
    return TypeModelLoader(external).load() ?: TypeModel()
  }

  private fun loadExternalInformation(): Map<String, Additional> {
    val map = HashMap<String, Additional>()
    val document = TypeModelLoader.getModelResourceXml("external-data.xml")
    // TODO: Populate map from xml
    return map
  }


  data class Additional(val name: String, val description: String? = null, val hint: String? = null)
}

private class TypeModelLoader(val external: Map<String, TypeModelProvider.Additional>) {
  public fun load(): TypeModel? {
    val model = TypeModel()
    try {
      val reflections = Reflections(ConfigurationBuilder()
          .setUrls(ClasspathHelper.forPackage("terraform.model", TypeModelLoader::class.java.classLoader))
          .setScanners(ResourcesScanner())
      );
      val resources = reflections.getResources(Pattern.compile(".*\\.json")).filter { it.contains("terraform/model/") };
      resources.forEach { it ->
        val file = if (it.startsWith('/')) it else "/$it"
        val json = getResourceJson(file) as JsonObject?
        if (json != null) {
          val pair = parseFile(json)
          model.providers.add(pair.first)
          model.resources.addAll(pair.second)
        } else {
          val msg = "Failed to load provider model from file '$file'"
          LOG.error(msg)
          if (ApplicationManager.getApplication().isUnitTestMode) {
            assert(false) { msg }
          }
        }
      }

      // TODO: Load & parse provisioners
      // TODO: Fetch latest model from github (?)
    } catch(e: Exception) {
      if (ApplicationManager.getApplication().isUnitTestMode) {
        LOG.error(e);
        assert(false) { "In unit test mode exceptions not tolerated. Exception: ${e.getMessage()}" }
      }
      LOG.warn(e)
      return null
    }
    return model
  }

  companion object {
    private final val LOG = Logger.getInstance(TypeModelLoader::class.java);

    fun getResource(path: String): InputStream? {
      return TypeModelProvider::class.java.getResourceAsStream(path)
    }

    fun getModelResource(path: String): InputStream? {
      return getResource("/terraform/model/$path")
    }

    fun getModelResourceXml(path: String): Document? {
      val stream = getModelResource(path) ?: return null
      return kotlin.dom.parseXml(stream)
    }

    fun getResourceJson(path: String): Any? {
      val stream = getResource(path) ?: return null
      val parser = Parser()
      return parser.parse(stream)
    }
  }

  private fun parseFile(json: JsonObject): Pair<ProviderType, List<ResourceType>> {
    val name = json.string("name")!!;
    val provider = json.obj("provider")!!;
    val resources = json.obj("resources")!!;
    return Pair(parseProviderInfo(name, provider), resources.map { parseResourceInfo(it) })
  }

  private fun parseProviderInfo(name: String, obj: JsonObject): ProviderType {
    return ProviderType(name, *obj.map { parseElement(it, name) }.toTypedArray());
  }

  private fun parseResourceInfo(entry: Map.Entry<String, Any?>): ResourceType {
    val name = entry.key
    assert(entry.value is JsonObject, { "Right part of provider should be object" })
    val obj = entry.value as JsonObject
    return ResourceType(name, *obj.map { parseElement(it, name) }.toTypedArray());
  }

  private fun parseElement(entry: Map.Entry<String, Any?>, providerName: String): PropertyOrBlockType {
    val name = entry.key;
    assert(entry.value is JsonArray<*>, { "Right part of provider resource should be array" })
    val obj = (entry.value as JsonArray<*>).filterIsInstance(JsonObject::class.java)
    val m = HashMap<String, JsonObject>();
    for (it in obj) {
      val n = it.string("name")
      if (n != null) {
        m.put(n, it);
      }
    }

    val type = parseType(m.get("Type"))
    val elem = m.get("Elem")
    if (elem != null) {
      // Valid only for TypeSet and TypeList, should parse internal structure
      // TODO: parse internal resource/schemas
      // populate typeHint using parsed value

      // ?? return BlockType(name).toPOBT()
    }
    val required = (m.get("Required")?.string("value")?.toLowerCase() ?: "false").toBoolean()

    val fqn = "$providerName.$name"
    val additional = external.get(fqn) ?: TypeModelProvider.Additional(name);
    return PropertyType(name, type, additional.hint, additional.description, required).toPOBT()
  }

  private fun parseType(attribute: JsonObject?): Type {
    // Fallback
    if (attribute == null) return Types.String;

    /*
    From  terraform/helper/schema/valuetype.go
    const (
            TypeInvalid ValueType = iota
            TypeBool
            TypeInt
            TypeFloat
            TypeString
            TypeList
            TypeMap
            TypeSet
            typeObject
    )
     */
    return when (attribute.string("value")) {
      "TypeInvalid" -> Types.Invalid
      "TypeBool" -> Types.Boolean
      "TypeInt" -> Types.Number
      "TypeFloat" -> Types.Number
      "TypeString" -> Types.String
      "TypeList" -> Types.Array
      "TypeSet" -> Types.Array
      "TypeMap" -> Types.Object
      else -> Types.Invalid
    }
  }
}

public class TypeModel {
  val resources: MutableList<ResourceType> = arrayListOf(
      //      ResourceType("aws_elb",
      //          PropertyType("name", Types.String).toPOBT(),
      //          PropertyType("availability_zones", Types.Array, "String").toPOBT(),
      //          PropertyType("instances", Types.Array, "String").toPOBT(),
      //          *DefaultResourceTypeProperties),
      //      ResourceType("aws_instance",
      //          PropertyType("instance_type", Types.String).toPOBT(),
      //          PropertyType("ami", Types.String).toPOBT(),
      //          *DefaultResourceTypeProperties)
  )
  val providers: MutableList<ProviderType> = arrayListOf(
      //      ProviderType("aws", PropertyOrBlockType(PropertyType("region", Types.String)), *DefaultProviderTypeProperties)
  )
  val variables: MutableList<VariableType> = arrayListOf()

  fun getResourceType(name: String): ResourceType? {
    return resources.firstOrNull { it.type == name }
  }

  fun getProviderType(name: String): ProviderType? {
    return providers.firstOrNull { it.type == name }
  }

  fun getBlockTypeNames(): List<String> {
    return listOf(
        "atlas",
        "module",
        "output",
        "provider",
        "resource",
        "variable"
    )
  }


}
