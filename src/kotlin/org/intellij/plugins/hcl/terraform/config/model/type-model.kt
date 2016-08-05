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
package org.intellij.plugins.hcl.terraform.config.model

import com.beust.klaxon.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern

// Model for element types

open class Type(val name: String)

open class BaseModelType(val description: String? = null, val required: Boolean = false, val deprecated: String? = null)

interface Hint
open class SimpleHint(vararg val hint: String) : Hint
open class TypeHint(val hint: Type) : Hint
open class ListHint(val hint: List<PropertyOrBlockType>) : Hint

// TODO: Use some 'Reference' class
open class ReferenceHint(vararg val hint: String) : Hint

// TODO: Use Interpolation result type
open class InterpolationHint(hint: String) : SimpleHint(hint)

open class PropertyType(val name: String, val type: Type, val hint: Hint? = null, val injectionAllowed: Boolean = true, description: String? = null, required: Boolean = false, deprecated: String? = null) : BaseModelType(description = description, required = required, deprecated = deprecated)

open class BlockType(val literal: String, val args: Int = 0, description: String? = null, required: Boolean = false, deprecated: String? = null, vararg val properties: PropertyOrBlockType = arrayOf()) : BaseModelType(description = description, required = required, deprecated = deprecated)

class PropertyOrBlockType private constructor(val property: PropertyType? = null, val block: BlockType? = null) {
  val name: String = if (property != null) property.name else block!!.literal
  val required: Boolean = if (property != null) property.required else block!!.required
  val deprecated: String? = if (property != null) property.deprecated else block!!.deprecated

  init {
    assert(property != null || block != null, { "Either property or block expected" });
  }

  constructor(property: PropertyType) : this(property, null)

  constructor(block: BlockType) : this(null, block)
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
}

class ResourceType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("resource", 2, properties = *properties)
class DataSourceType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("data", 2, properties = *properties)
class ProviderType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("provider", 1, properties = *properties)
class ProvisionerType(val type: String, vararg properties: PropertyOrBlockType = arrayOf()) : BlockType("provisioner", 1, properties = *properties)

// region Interpolation Functions
open class Argument(val type: Type, val name: String? = null)
open class VariadicArgument(type: Type, name: String? = null) : Argument(type, name)

class Function(val name: String, val ret: Type, vararg val arguments: Argument = arrayOf(), val variadic: VariadicArgument? = null) {
  init {
    val count = arguments.count { it is VariadicArgument }
    assert (count == 0 || (count == 1 && arguments.last() is VariadicArgument)) { "Only one (last) argument could be variadic" }
  }
}
// endregion



class TypeModelProvider {
  val model: TypeModel by lazy { loadModel() }
  val external: Map<String, Additional> by lazy { loadExternalInformation() }

  fun get(): TypeModel = model

  private fun loadModel(): TypeModel {
    return TypeModelLoader(external).load() ?: TypeModel()
  }

  private fun loadExternalInformation(): Map<String, Additional> {
    val map = HashMap<String, Additional>()
    val json = TypeModelLoader.getModelExternalInformation("external-data.json") as JsonObject?
    // TODO: Populate map from json
    return map
  }


  data class Additional(val name: String, val description: String? = null, val hint: String? = null)
}

private class TypeModelLoader(val external: Map<String, TypeModelProvider.Additional>) {

  val resources: MutableList<ResourceType> = arrayListOf()
  val dataSources: MutableList<DataSourceType> = arrayListOf()
  val providers: MutableList<ProviderType> = arrayListOf()
  val provisioners: MutableList<ProvisionerType> = arrayListOf()
  val functions: MutableList<Function> = arrayListOf()

  fun load(): TypeModel? {
    val application = ApplicationManager.getApplication()
    try {
      val reflections = Reflections(ConfigurationBuilder()
          .setUrls(ClasspathHelper.forPackage("terraform.model", TypeModelLoader::class.java.classLoader))
          .setScanners(ResourcesScanner())
      );
      val resources = reflections.getResources(Pattern.compile(".*\\.json")).filter { it.contains("terraform/model/") };
      resources.forEach { it ->
        val file = it.ensureHavePrefix("/")
        val json = getResourceJson(file) as JsonObject?
        if (json != null) {
          try {
            parseFile(json, file)
          } catch(e: Throwable) {
            val msg = "Failed to parse file '$file'"
            LOG.error(msg, e)
            if (application.isUnitTestMode || application.isInternal) {
              assert(false) { msg }
            }
          }
        } else {
          val msg = "Failed to load anything from file '$file'"
          LOG.error(msg)
          if (application.isUnitTestMode || application.isInternal) {
            assert(false) { msg }
          }
        }
      }

      // TODO: Fetch latest model from github (?)

      return TypeModel(this.resources, this.dataSources, this.providers, this.provisioners, this.functions)
    } catch(e: Exception) {
      if (application.isUnitTestMode || application.isInternal) {
        LOG.error(e);
        assert(false) { "In unit test mode exceptions are not tolerated. Exception: ${e.message}" }
      }
      LOG.warn(e)
      return null
    }
  }

  companion object {
    private final val LOG = Logger.getInstance(TypeModelLoader::class.java);

    fun getResource(path: String): InputStream? {
      return TypeModelProvider::class.java.getResourceAsStream(path)
    }

    fun getModelExternalInformation(path: String): Any? {
      return getResourceJson("/terraform/model-external/$path")
    }

    fun getResourceJson(path: String): Any? {
      val stream = getResource(path) ?: return null
      val parser = Parser()
      return parser.parse(stream)
    }
  }

  private fun parseFile(json: JsonObject, file: String) {
    val type = json.string("type");
    when (type) {
      "provisioner" -> return parseProvisionerFile(json, file)
      "provider" -> return parseProviderFile(json, file)
      "functions" -> return parseInterpolationFunctions(json, file)
    }
    LOG.warn("Cannot determine model file content, $file")
  }

  private fun parseProviderFile(json: JsonObject, file: String) {
    val name = json.string("name")!!;
    val provider = json.obj("schema");
    if (provider == null) {
      LOG.warn("No provider schema in file '$file'")
      return
    }
    val info = parseProviderInfo(name, provider)
    this.providers.add(info)
    val resources = json.obj("resources");
    if (resources == null) {
      LOG.warn("No resources for provider '$name' in file '$file'")
      return
    }
    val map = resources.map { parseResourceInfo(it) }
    this.resources.addAll(map)
    val dataSources = json.obj("data-sources")
    if (dataSources == null) {
      LOG.warn("No data-sources for provider '$name' in file '$file'")
      return
    }
    this.dataSources.addAll(dataSources.map { parseDataSourceInfo(it) })
  }

  private fun parseProvisionerFile(json: JsonObject, file: String) {
    val name = json.string("name")!!;
    val provisioner = json.obj("schema");
    if (provisioner == null) {
      LOG.warn("No provisioner schema in file '$file'")
      return
    }
    val info = parseProvisionerInfo(name, provisioner);
    this.provisioners.add(info)
  }

  private fun parseInterpolationFunctions(json: JsonObject, file: String) {
    val functions = json.obj("schema");
    if (functions == null) {
      LOG.warn("No functions schema in file '$file'")
      return
    }
    for ((k, v) in functions) {
      if (v !is JsonObject) continue;
      assert(v.string("Name").equals(k)) { "Name mismatch: $k != ${v.string("Name")}" }
      val returnType = parseType(v.string("ReturnType")!!)
      val args = v.array<String>("ArgTypes")!!.map { parseType(it) }.map { Argument(it) }.toMutableList()
      val variadic = v.boolean("Variadic") ?: false
      var va: VariadicArgument? = null
      if (variadic) {
        va = VariadicArgument(parseType(v.string("VariadicType")))
      }
      this.functions.add(Function(k, returnType, *args.toTypedArray(), variadic = va))
    }
  }

  private fun parseProviderInfo(name: String, obj: JsonObject): ProviderType {
    return ProviderType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray());
  }

  private fun parseProvisionerInfo(name: String, obj: JsonObject): ProvisionerType {
    return ProvisionerType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray());
  }

  private fun parseResourceInfo(entry: Map.Entry<String, Any?>): ResourceType {
    val name = entry.key
    assert(entry.value is JsonObject, { "Right part of resource should be object" })
    val obj = entry.value as JsonObject
    return ResourceType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray());
  }

  private fun parseDataSourceInfo(entry: Map.Entry<String, Any?>): DataSourceType {
    val name = entry.key
    assert(entry.value is JsonObject, { "Right part of data-source should be object" })
    val obj = entry.value as JsonObject
    return DataSourceType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray());
  }

  private fun parseSchemaElement(entry: Map.Entry<String, Any?>, providerName: String): PropertyOrBlockType {
    return parseSchemaElement(entry.key, entry.value, providerName)
  }

  private fun parseSchemaElement(name: String, value: Any?, providerName: String): PropertyOrBlockType {
    assert(value is JsonArray<*>, { "Right part of schema element (field parameters) should be array" })
    val obj = (value as JsonArray<*>).filterIsInstance(JsonObject::class.java)
    val m = HashMap<String, JsonObject>();
    for (it in obj) {
      val n = it.string("name")
      if (n != null) {
        m.put(n, it);
      }
    }

    val fqn = "$providerName.$name"

    var hint: Hint? = null

    var isBlock = false

    val type = parseType(m["Type"])
    val elem = m["Elem"]
    if (elem != null) {
      // Valid only for TypeSet and TypeList, should parse internal structure
      // TODO: ensure set only for TypeSet and TypeList
      val et = elem.string("type")
      if (et == "ResourceSchemaElements") {
        val a = elem.array<Any>("value")
        if (a != null) {
          val parsed = parseSchemaElement("__inner__", a, "$fqn")
          hint = parsed.property?.type?.let { TypeHint(it) } ?: parsed.block?.properties?.let { ListHint(it.asList()) }
        }
      }
      if (et == "ResourceSchemaInfo") {
        val o = elem.obj("value")
        if (o != null) {
          val innerTypeProperties = o.map { parseSchemaElement(it, "$fqn") }
          hint = ListHint(innerTypeProperties)
          if (type == Types.Array) {
            isBlock = true
          }
        }
      }
      // ?? return BlockType(name).toPOBT()
    }
    val required = (m["Required"]?.string("value")?.toLowerCase() ?: "false").toBoolean()
    val deprecated = m["Deprecated"]?.string("value") ?: null

    val additional = external[fqn] ?: TypeModelProvider.Additional(name);

    if (type == Types.Object) {
      isBlock = true
    }

    // External description and hint overrides one from model
    if (isBlock) {
      // TODO: Do something with a additional.hint
      var bh: Array<out PropertyOrBlockType> = emptyArray()
      if (hint is ListHint) {
        bh = hint.hint.toTypedArray()
      }
      return BlockType(name, required = required,
          deprecated = deprecated,
          description = additional.description ?: m["Description"]?.string("value") ?: null, properties = *bh).toPOBT()
    }
    return PropertyType(name, type, hint = additional.hint?.let { SimpleHint(it) } ?: hint,
        description = additional.description ?: m["Description"]?.string("value") ?: null,
        required = required,
        deprecated = deprecated).toPOBT()
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
    return parseType(attribute.string("value"))
  }

  private fun parseType(string: String?): Type {
    return when (string) {
      "TypeInvalid" -> Types.Invalid
      "TypeBool" -> Types.Boolean
      "TypeInt" -> Types.Number
      "TypeFloat" -> Types.Number
      "TypeString" -> Types.String
      "TypeList" -> Types.Array
      "TypeSet" -> Types.Array
      "TypeMap" -> Types.Object
      "TypeAny" -> Types.Any
      else -> Types.Invalid
    }
  }
}

class TypeModel(
    val resources: List<ResourceType> = arrayListOf(),
    val dataSources: List<DataSourceType> = arrayListOf(),
    val providers: List<ProviderType> = arrayListOf(),
    val provisioners: List<ProvisionerType> = arrayListOf(),
    val functions: List<Function> = arrayListOf()
) {
  companion object {
    val Atlas: BlockType = BlockType("atlas", 0, properties = PropertyType("name", Types.String, injectionAllowed = false, required = true).toPOBT())
    val Module: BlockType = BlockType("module", 1, properties = PropertyType("source", Types.String, hint = SimpleHint("Url"), required = true).toPOBT())
    val Output: BlockType = BlockType("output", 1, properties = PropertyType("value", Types.String, hint = InterpolationHint("Any"), required = true).toPOBT())

    val Variable_Type = PropertyType("type", Types.String, SimpleHint("string", "list", "map"), false)
    val Variable_Default = PropertyType("default", Type("String|Array|Object"))
    val Variable_Description = PropertyType("description", Types.String)
    val Variable: BlockType = BlockType("variable", 1, properties = *arrayOf(
        Variable_Type.toPOBT(),
        Variable_Default.toPOBT(),
        Variable_Description.toPOBT()
    ))

    val Connection: BlockType = BlockType("connection", 0, properties = *arrayOf(
        PropertyType("type", Types.String, description = "The connection type that should be used. Valid types are \"ssh\" and \"winrm\" This defaults to \"ssh\".").toPOBT(),
        PropertyType("user", Types.String).toPOBT(),
        PropertyType("password", Types.String).toPOBT(),
        PropertyType("host", Types.String).toPOBT(),
        PropertyType("port", Types.Number).toPOBT(),
        PropertyType("timeout", Types.String).toPOBT(),
        PropertyType("script_path", Types.String).toPOBT()
    ))
    val ConnectionPropertiesSSH: Array<out PropertyOrBlockType> = arrayOf(
        // ssh
        PropertyType("key_file", Types.String, deprecated = "Use 'private_key'").toPOBT(),
        PropertyType("private_key", Types.String).toPOBT(),
        PropertyType("agent", Types.Boolean).toPOBT(),

        // bastion ssh
        PropertyType("bastion_host", Types.String).toPOBT(),
        PropertyType("bastion_port", Types.Number).toPOBT(),
        PropertyType("bastion_user", Types.String).toPOBT(),
        PropertyType("bastion_password", Types.String).toPOBT(),
        PropertyType("bastion_private_key", Types.String).toPOBT(),
        PropertyType("bastion_key_file", Types.String, deprecated = "Use 'bastion_private_key'").toPOBT()
    );
    val ConnectionPropertiesWinRM: Array<out PropertyOrBlockType> = arrayOf(
        // winrm
        PropertyType("https", Types.Boolean).toPOBT(),
        PropertyType("insecure", Types.Boolean).toPOBT(),
        PropertyType("cacert", Types.String).toPOBT()
    );

    val ResourceLifecycle: BlockType = BlockType("lifecycle", 0,
        description = "Describe to Terraform how to connect to the resource for provisioning", // TODO: Improve description
        properties = *arrayOf(
            PropertyType("create_before_destroy", Types.Boolean).toPOBT(),
            PropertyType("prevent_destroy", Types.Boolean).toPOBT(),
            PropertyType("ignore_changes", Types.Array, hint = TypeHint(Types.String)).toPOBT()
        ))
    val AbstractResourceProvisioner: BlockType = BlockType("provisioner", 1, properties = *arrayOf(
        Connection.toPOBT()
    ))

    @JvmField val AbstractResource: BlockType = BlockType("resource", 2, properties = *arrayOf(
        PropertyType("id", Types.String, injectionAllowed = false, description = "A unique ID for this resource", required = false).toPOBT(),
        PropertyType("count", Types.Number).toPOBT(),
        PropertyType("depends_on", Types.Array, hint = TypeHint(Types.String)).toPOBT(),
        PropertyType("provider", Types.String, hint = ReferenceHint("provider.type", "provider.alias")).toPOBT(),
        ResourceLifecycle.toPOBT(),
        // Also may have connection? and provisioner+ blocks
        Connection.toPOBT(),
        AbstractResourceProvisioner.toPOBT()
    ))
    @JvmField val AbstractDataSource: BlockType = BlockType("data", 2, properties = *arrayOf(
        PropertyType("id", Types.String, injectionAllowed = false, description = "A unique ID for this data source", required = false).toPOBT(),
        PropertyType("count", Types.Number).toPOBT(),
        PropertyType("depends_on", Types.Array, hint = TypeHint(Types.String)).toPOBT(),
        PropertyType("provider", Types.String, hint = ReferenceHint("provider.type", "provider.alias")).toPOBT()
    ))
    val AbstractProvider: BlockType = BlockType("provider", 1, required = false, properties = *arrayOf(
        PropertyType("alias", Types.String, injectionAllowed = false).toPOBT())
    )

    val RootBlocks = listOf(Atlas, Module, Output, Variable, AbstractProvider, AbstractResource, AbstractDataSource)
    val RootBlocksMap = RootBlocks.map { it.literal to it }.toMap()
  }

  fun getResourceType(name: String): ResourceType? {
    return resources.firstOrNull { it.type == name }
  }

  fun getDataSourceType(name: String): DataSourceType? {
    return dataSources.firstOrNull { it.type == name }
  }

  fun getProviderType(name: String): ProviderType? {
    return providers.firstOrNull { it.type == name }
  }

  fun getProvisionerType(name: String): ProvisionerType? {
    return provisioners.firstOrNull { it.type == name }
  }
}
