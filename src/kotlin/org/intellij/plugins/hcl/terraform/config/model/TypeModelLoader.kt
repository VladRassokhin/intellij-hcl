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

import com.beust.klaxon.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.util.*

class TypeModelLoader(val external: Map<String, TypeModelProvider.Additional>) {

  val resources: MutableList<ResourceType> = arrayListOf()
  val dataSources: MutableList<DataSourceType> = arrayListOf()
  val providers: MutableList<ProviderType> = arrayListOf()
  val provisioners: MutableList<ProvisionerType> = arrayListOf()
  val functions: MutableList<Function> = arrayListOf()

  fun load(): TypeModel? {
    val application = ApplicationManager.getApplication()
    try {
      val providers = loadList("/terraform/model/providers.list")?.map { "providers/$it.json" } ?: emptyList()
      val provisioners = loadList("/terraform/model/provisioners.list")?.map { "provisioners/$it.json" } ?: emptyList()
      val resources: Collection<String> = (providers + provisioners + "functions.json").map { "/terraform/model/" + it }

      resources.forEach {
        val file = it.ensureHavePrefix("/")
        val json: JsonObject?
        try {
          json = getResourceJson(file) as JsonObject?
        } catch(e: Exception) {
          val msg = "Failed to load json data from file '$file'"
          LOG.error(msg, e)
          if (application.isUnitTestMode || application.isInternal) {
            assert(false) { msg }
          }
          return@forEach
        }
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
        LOG.error(e)
        assert(false) { "In unit test mode exceptions are not tolerated. Exception: ${e.message}" }
      }
      LOG.warn(e)
      return null
    }
  }

  companion object {
    internal val LOG = Logger.getInstance(TypeModelLoader::class.java)

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

  private fun loadList(name: String): Set<String>? {
    try {
      val stream = getResource(name)
      if (stream == null) {
        val message = "Cannot read list '$name': resource not found"
        LOG.warn(message)
        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode || application.isInternal) {
          assert(false) { message }
        }
        return emptySet()
      }
      val lines = stream.bufferedReader(Charsets.UTF_8).readLines().map { it.trim() }.filter { !it.isEmpty() }
      return LinkedHashSet<String>(lines)
    } catch(e: Exception) {
      LOG.warn("Cannot read 'ignored-references.list': ${e.message}")
      return emptySet()
    }
  }

  private fun parseFile(json: JsonObject, file: String) {
    val type = json.string("type")
    when (type) {
      "provisioner" -> return parseProvisionerFile(json, file)
      "provider" -> return parseProviderFile(json, file)
      "functions" -> return parseInterpolationFunctions(json, file)
    }
    LOG.warn("Cannot determine model file content, $file")
  }

  private fun parseProviderFile(json: JsonObject, file: String) {
    val name = json.string("name")!!
    val provider = json.obj("schema")
    if (provider == null) {
      LOG.warn("No provider schema in file '$file'")
      return
    }
    val info = parseProviderInfo(name, provider)
    this.providers.add(info)
    val resources = json.obj("resources")
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
    val name = json.string("name")!!
    val provisioner = json.obj("schema")
    if (provisioner == null) {
      LOG.warn("No provisioner schema in file '$file'")
      return
    }
    val info = parseProvisionerInfo(name, provisioner)
    this.provisioners.add(info)
  }

  private fun parseInterpolationFunctions(json: JsonObject, file: String) {
    val functions = json.obj("schema")
    if (functions == null) {
      LOG.warn("No functions schema in file '$file'")
      return
    }
    for ((k, v) in functions) {
      if (v !is JsonObject) continue
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
    return ProviderType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseProvisionerInfo(name: String, obj: JsonObject): ProvisionerType {
    return ProvisionerType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseResourceInfo(entry: Map.Entry<String, Any?>): ResourceType {
    val name = entry.key
    assert(entry.value is JsonObject, { "Right part of resource should be object" })
    val obj = entry.value as JsonObject
    return ResourceType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseDataSourceInfo(entry: Map.Entry<String, Any?>): DataSourceType {
    val name = entry.key
    assert(entry.value is JsonObject, { "Right part of data-source should be object" })
    val obj = entry.value as JsonObject
    return DataSourceType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseSchemaElement(entry: Map.Entry<String, Any?>, providerName: String): PropertyOrBlockType {
    return parseSchemaElement(entry.key, entry.value, providerName)
  }

  private fun parseSchemaElement(name: String, value: Any?, providerName: String): PropertyOrBlockType {
    assert(value is JsonArray<*>, { "Right part of schema element (field parameters) should be array" })
    val obj = (value as JsonArray<*>).filterIsInstance(JsonObject::class.java)
    val m = HashMap<String, JsonObject>()
    for (it in obj) {
      val n = it.string("name")
      if (n != null) {
        m.put(n, it)
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
          val parsed = parseSchemaElement("__inner__", a, fqn)
          hint = parsed.property?.type?.let { TypeHint(it) } ?: parsed.block?.properties?.let { ListHint(it.asList()) }
        }
      }
      if (et == "ResourceSchemaInfo") {
        val o = elem.obj("value")
        if (o != null) {
          val innerTypeProperties = o.map { parseSchemaElement(it, fqn) }
          hint = ListHint(innerTypeProperties)
          if (type == Types.Array) {
            isBlock = true
          }
        }
      }
      // ?? return BlockType(name).toPOBT()
    }
    val deprecated = m["Deprecated"]?.string("value")
    val has_default: Boolean =
        m["Default"]?.string("value") != null
            || m["DefaultValue_Computed"]?.string("value") != null
    // || m["InputDefault"]?.string("value") != null // Not sure about this property TODO: Investigate how it works in terraform

    val additional = external[fqn] ?: TypeModelProvider.Additional(name)
    // TODO: Consider move 'has_default' to Additional

    val required = additional.required ?: m["Required"]?.string("value")?.toLowerCase()?.toBoolean() ?: false

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
          description = additional.description ?: m["Description"]?.string("value"), properties = *bh).toPOBT()
    }
    return PropertyType(name, type, hint = additional.hint?.let { SimpleHint(it) } ?: hint,
        description = additional.description ?: m["Description"]?.string("value"),
        required = required,
        deprecated = deprecated,
        has_default = has_default).toPOBT()
  }

  private fun parseType(attribute: JsonObject?): Type {
    // Fallback
    if (attribute == null) return Types.String

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