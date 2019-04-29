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
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import org.intellij.plugins.hcl.terraform.config.Constants
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

class TypeModelLoader(val external: Map<String, TypeModelProvider.Additional>) {

  val resources: MutableList<ResourceType> = arrayListOf()
  val dataSources: MutableList<DataSourceType> = arrayListOf()
  val providers: MutableList<ProviderType> = arrayListOf()
  val provisioners: MutableList<ProvisionerType> = arrayListOf()
  val backends: MutableList<BackendType> = arrayListOf()
  val functions: MutableList<Function> = arrayListOf()

  val loaded: MutableMap<String, String> = linkedMapOf()

  fun load(): TypeModel? {
    val application = ApplicationManager.getApplication()
    try {
      loadExternal(application)
      loadBundled(application)

      this.resources.sortBy { it.type }
      this.dataSources.sortBy { it.type }
      this.providers.sortBy { it.type }
      this.provisioners.sortBy { it.type }
      this.backends.sortBy { it.type }
      this.functions.sortBy { it.name }

      // TODO: Fetch latest model from github (?)
      return TypeModel(
          this.resources.associateBy { it.type },
          this.dataSources.associateBy { it.type },
          this.providers.associateBy { it.type },
          this.provisioners.associateBy { it.type },
          this.backends.associateBy { it.type },
          this.functions.associateBy { it.name }
      )
    } catch(e: Exception) {
      logErrorAndFailInInternalMode(application, "Failed to load Terraform Model", e)
      return null
    }
  }

  private fun loadBundled(application: Application) {
    val resources: Collection<String> = getAllResourcesToLoad(ModelResourcesPrefix)

    for (it in resources) {
      val file = it.ensureHavePrefix("/")
      val stream = getResource(file)
      if (stream == null) {
        LOG.warn("Resource '$file' was not found")
        continue
      }

      loadOne(application, file, stream)
    }
  }

  private fun loadExternal(application: Application) {
    val schemas = getSharedSchemas()
    for (file in schemas) {
      val stream: FileInputStream
      try {
        stream = file.inputStream()
      } catch (e: Exception) {
        logErrorAndFailInInternalMode(application, "Cannot open stream for file '${file.absolutePath}'", e)
        continue
      }
      loadOne(application, file.absolutePath, stream)
    }
  }

  private fun loadOne(application: Application, file: String, stream: InputStream) {
    val json: JsonObject?
    try {
      json = stream.use {
        val parser = Parser()
        parser.parse(stream) as JsonObject?
      }
      if (json == null) {
        logErrorAndFailInInternalMode(application, "In file '$file' no JSON found")
        return
      }
    } catch(e: Exception) {
      logErrorAndFailInInternalMode(application, "Failed to load json data from file '$file'", e)
      return
    }
    try {
      parseFile(json, file)
    } catch(e: Throwable) {
      logErrorAndFailInInternalMode(application, "Failed to parse file '$file'", e)
    }
    return
  }

  private fun logErrorAndFailInInternalMode(application: Application, msg: String, e: Throwable? = null) {
    val msg2 = if (e == null) msg else "$msg: ${e.message}"
    if (e == null) LOG.error(msg2) else LOG.error(msg2, e)
    if (application.isInternal) {
      throw AssertionError(msg2, e)
    }
  }

  private fun getSharedSchemas(): List<File> {
    val terraform_d: File = getGlobalTerraformDir() ?: return emptyList()

    val result = ArrayList<File>()

    val schemas = File(terraform_d, "schemas")
    if (schemas.exists() && schemas.isDirectory) {
      FileUtil.processFilesRecursively(schemas) {
        if (it.isFile && it.name.endsWith(".json", ignoreCase = true)) {
          result.add(it)
        }
        return@processFilesRecursively true
      }
    }

    val metadataRepo = File(terraform_d, "metadata-repo/terraform/model")
    if (metadataRepo.exists() && metadataRepo.isDirectory) {
      FileUtil.processFilesRecursively(metadataRepo) {
        if (it.isFile && it.name.endsWith(".json", ignoreCase = true)) {
          result.add(it)
        }
        return@processFilesRecursively true
      }
    }

    return result
  }

  companion object {
    internal val LOG by lazy { Logger.getInstance(TypeModelLoader::class.java) }
    val ModelResourcesPrefix = "/terraform/model"

    fun getResource(path: String): InputStream? {
      return TypeModelProvider::class.java.getResourceAsStream(path)
    }

    fun getModelExternalInformation(path: String): Any? {
      return getResourceJson("/terraform/model-external/$path")
    }

    @Throws(RuntimeException::class, NullPointerException::class)
    fun getResourceJson(path: String): Any? {
      val stream = getResource(path) ?: return null
      stream.use {
        val parser = Parser()
        return parser.parse(stream)
      }
    }

    internal fun getAllResourcesToLoad(prefix: String): Collection<String> {
      val resources = ArrayList<String>()
      loadList("$prefix/providers.list")?.map { "$prefix/providers/$it.json" }?.toCollection(resources)
      loadList("$prefix/provisioners.list")?.map { "$prefix/provisioners/$it.json" }?.toCollection(resources)
      loadList("$prefix/backends.list")?.map { "$prefix/backends/$it.json" }?.toCollection(resources)
      resources.add("$prefix/functions.json")
      return resources
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

    fun getGlobalTerraformDir(): File? {
      val terraform_d = if (SystemInfo.isWindows) {
        System.getenv("APPDATA")?.let { File(it, "terraform.d") }
      } else {
        val userHome = SystemProperties.getUserHome()
        File(userHome, ".terraform.d")
      }
      if (terraform_d == null || !terraform_d.exists() || !terraform_d.isDirectory) return null
      return terraform_d
    }

    fun loadExternalResource(name: String): InputStream? {
      val stream: InputStream? = getGlobalTerraformDir()?.let { tf ->
        listOf(
            File(tf, "schemas/$name"),
            File(tf, "metadata-repo/terraform/model-external/$name")
        ).firstOrNull { it.exists() && it.isFile }?.let {
          try {
            it.inputStream()
          } catch (e: Exception) {
            LOG.warn("Cannot open stream for file '${it.absolutePath}'", e)
            null
          }
        }
      } ?: getResource("/terraform/model-external/$name")
      return stream
    }
  }

  private fun parseFile(json: JsonObject, file: String) {
    val type = json.string("type")
    when (type) {
      "provisioner" -> return parseProvisionerFile(json, file)
      "provider" -> return parseProviderFile(json, file)
      "functions" -> return parseInterpolationFunctions(json, file)
      "backend" -> return parseBackendFile(json, file)
    }
    LOG.warn("Cannot determine model file content, $file")
  }

  private fun parseProviderFile(json: JsonObject, file: String) {
    val name = json.string("name")!!.pool()
    val provider = json.obj("provider")
    if (provider == null) {
      LOG.warn("No provider schema in file '$file'")
      return
    }
    if (loaded.containsKey("provider.$name")) {
      LOG.warn("Provider '$name' is already loaded from '${loaded["provider.$name"]}'")
      return
    }
    loaded["provider.$name"] = file
    val info = parseProviderInfo(name, provider)
    this.providers.add(info)
    val resources = json.obj("resources")
    val dataSources = json.obj("data-sources")
    if (resources == null && dataSources == null) {
      LOG.warn("No resources nor data-sources defined for provider '$name' in file '$file'")
    }
    resources?.let { it.mapTo(this.resources) { parseResourceInfo(it, info) } }
    dataSources?.let { it.mapTo(this.dataSources) { parseDataSourceInfo(it, info) } }
  }

  private fun parseProvisionerFile(json: JsonObject, file: String) {
    val name = json.string("name")!!.pool()
    val provisioner = json.obj("schema")
    if (provisioner == null) {
      LOG.warn("No provisioner schema in file '$file'")
      return
    }
    if (loaded.containsKey("provisioner.$name")) {
      LOG.warn("Provisioner '$name' is already loaded from '${loaded["provisioner.$name"]}'")
      return
    }
    loaded["provisioner.$name"] = file
    val info = parseProvisionerInfo(name, provisioner)
    this.provisioners.add(info)
  }

  private fun parseBackendFile(json: JsonObject, file: String) {
    val name = json.string("name")!!.pool()
    val backend = json.obj("schema")
    if (backend == null) {
      LOG.warn("No backend schema in file '$file'")
      return
    }
    if (loaded.containsKey("backend.$name")) {
      LOG.warn("Backend '$name' is already loaded from '${loaded["backend.$name"]}'")
      return
    }
    loaded["backend.$name"] = file
    val info = parseBackendInfo(name, backend)
    this.backends.add(info)
  }

  private fun parseInterpolationFunctions(json: JsonObject, file: String) {
    val functions = json.obj("schema")
    if (functions == null) {
      LOG.warn("No functions schema in file '$file'")
      return
    }
    if (loaded.containsKey("functions")) {
      LOG.warn("Functions definitions already loaded from '${loaded["functions"]}'")
      return
    }
    loaded["functions"] = file
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
      this.functions.add(Function(k.pool(), returnType, *args.toTypedArray(), variadic = va))
    }
  }

  private fun parseProviderInfo(name: String, obj: JsonObject): ProviderType {
    return ProviderType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseProvisionerInfo(name: String, obj: JsonObject): ProvisionerType {
    return ProvisionerType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseBackendInfo(name: String, obj: JsonObject): BackendType {
    return BackendType(name, *obj.map { parseSchemaElement(it, name) }.toTypedArray())
  }

  private fun parseResourceInfo(entry: Map.Entry<String, Any?>, info: ProviderType): ResourceType {
    val name = entry.key.pool()
    assert(entry.value is JsonObject) { "Right part of resource should be object" }
    val obj = entry.value as JsonObject
    val timeouts = getTimeoutsBlock(obj)
    return ResourceType(name, info, *obj.map { parseSchemaElement(it, name) }.plus(timeouts).filterNotNull().toTypedArray())
  }

  private fun parseDataSourceInfo(entry: Map.Entry<String, Any?>, info: ProviderType): DataSourceType {
    val name = entry.key.pool()
    assert(entry.value is JsonObject) { "Right part of data-source should be object" }
    val obj = entry.value as JsonObject
    val timeouts = getTimeoutsBlock(obj)
    return DataSourceType(name, info, *obj.map { parseSchemaElement(it, name) }.plus(timeouts).filterNotNull().toTypedArray())
  }

  private fun getTimeoutsBlock(obj: JsonObject): PropertyOrBlockType? {
    val value = obj.remove(Constants.TIMEOUTS) ?: return null
    assert(value is JsonArray<*>) {"${Constants.TIMEOUTS} should be an array"}
    val array = value as? JsonArray<*> ?: return null
    for (element in array) {
      assert(element is String) {"${Constants.TIMEOUTS} array elements should be string, got ${element?.javaClass?.name}"}
    }
    val timeouts = array.map { it.toString().pool() }
    if (timeouts.isEmpty()) return null
    return BlockType("timeouts", 0,
        description = "Amount of time a specific operation is allowed to take before being considered an error", // TODO: Improve description
        properties = *timeouts.map { PropertyType(it, Types.String).pool() }.toTypedArray()
        // TODO: ^ Check type, should be Time? (allowed suffixes are s, m, h)
    ).pool()
  }

  private fun parseSchemaElement(entry: Map.Entry<String, Any?>, providerName: String): PropertyOrBlockType {
    return parseSchemaElement(entry.key, entry.value, providerName)
  }

  private fun parseSchemaElement(name: String, value: Any?, providerName: String): PropertyOrBlockType {
    assert(value is JsonObject) { "Right part of schema element (field parameters) should be object" }
    if (value !is JsonObject) {
      throw IllegalStateException()
    }

    val fqn = "$providerName.$name"

    var hint: Hint? = null

    var isBlock = false

    if (name == Constants.TIMEOUTS) {
      throw IllegalStateException(Constants.TIMEOUTS + " not expected here")
    }

    val type = parseType(value.string("Type"))
    val elem = value.obj("Elem")
    if (elem != null && elem.isNotEmpty()) {
      // Valid only for TypeSet and TypeList, should parse internal structure
      // TODO: ensure set only for TypeSet and TypeList
      val et = elem.string("Type")?: elem.string("type")
      if (et == "SchemaElements") {
        val elements_type = elem.string("ElementsType") ?: elem.string("elements-type")
        if (elements_type != null) {
          hint = TypeHint(parseType(elements_type)).pool()
        }
      } else if (et == "SchemaInfo") {
        val o = elem.obj("Info") ?: elem.obj("info")
        if (o != null) {
          val innerTypeProperties = o.map { parseSchemaElement(it, fqn) }
          hint = ListHint(innerTypeProperties).pool()
          if (type == Types.Array) {
            isBlock = true
          }
        }
      } else if (et == null) {
        /*
          Something like with 'Value' == 'String':
          dimensions = {
            instanceId = "i-bp1247jeep0y53nu3bnk,i-bp11gdcik8z6dl5jm84p"
            device = "/dev/vda1,/dev/vdb1"
          }
         */
        val t = elem.string("Type") ?: elem.string("type")
        if (t != null) {
          //hint = TypeHint(parseType(t))
        }
      }
      // ?? return BlockType(name).toPOBT()
    }

    val conflicts: List<String>? = value.array<String>("ConflictsWith")?.map { it.pool() }

    val deprecated = value.string("Deprecated")
    val has_default: Boolean = value.obj("Default")?.isNotEmpty() ?: false
    val has_default_function: Boolean = value.string("DefaultFunc")?.isNotEmpty() ?: false
    // || m["InputDefault"]?.string("value") != null // Not sure about this property TODO: Investigate how it works in terraform

    val additional = external[fqn] ?: TypeModelProvider.Additional(name)
    // TODO: Consider move 'has_default' to Additional

    val required = additional.required ?: value.boolean("Required") ?: false
    val computed = value.boolean("Computed") ?: false

    if (type == Types.Object) {
      isBlock = true
    }

    val description = additional.description ?: value.string("Description")

    // External description and hint overrides one from model
    if (isBlock) {
      // TODO: Do something with a additional.hint
      var bh: Array<out PropertyOrBlockType> = PropertyOrBlockType.EMPTY_ARRAY
      if (hint is ListHint && hint.hint.isNotEmpty()) {
        bh = hint.hint.toTypedArray()
      }
      return BlockType(name.pool(), required = required,
          deprecated = deprecated?.pool(),
          computed = computed,
          description = description?.pool(),
          conflictsWith = conflicts,
          properties = *bh).pool()
    }
    return PropertyType(name.pool(), type, hint = additional.hint ?: hint,
        description = description?.pool(),
        required = required,
        deprecated = deprecated?.pool(),
        computed = computed,
        conflictsWith = conflicts,
        has_default = has_default || has_default_function).pool()
  }

  private fun parseType(string: String?): Type {
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
    return when (string?.removePrefix("Type")) {
      "Invalid" -> Types.Invalid
      "Bool" -> Types.Boolean
      "Int" -> Types.Number
      "Float" -> Types.Number
      "String" -> Types.String
      "List" -> Types.Array
      "Set" -> Types.Array
      "Map" -> Types.Object
      "Any" -> Types.Any
      else -> Types.Invalid
    }
  }

  //region object pools
  private val strings: MutableMap<String, String> = HashMap()
  private val properties: MutableMap<PropertyType, PropertyType> = HashMap()
  private val blocks: MutableMap<BlockType, BlockType> = HashMap()
  private val hints: MutableMap<Hint, Hint> = HashMap()

  private fun String.pool(): String {
    var ret = strings[this]
    if (ret != null) return ret
    ret = this
    strings[ret] = ret
    return ret
  }

  private fun PropertyType.pool(): PropertyType {
    var ret = properties[this]
    if (ret != null) return ret
    ret = this
    properties[ret] = ret
    return ret
  }

  private fun BlockType.pool(): BlockType {
    var ret = blocks[this]
    if (ret != null) return ret
    ret = this
    blocks[ret] = ret
    return ret
  }

  private fun Hint.pool(): Hint {
    var ret = hints[this]
    if (ret != null) return ret
    ret = this
    hints[ret] = ret
    return ret
  }
  //endregion
}