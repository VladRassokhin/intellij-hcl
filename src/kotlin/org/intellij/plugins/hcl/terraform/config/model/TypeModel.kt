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

class TypeModel(
    val resources: Map<String, ResourceType> = LinkedHashMap(),
    val dataSources: Map<String, DataSourceType> = LinkedHashMap(),
    val providers: Map<String, ProviderType> = LinkedHashMap(),
    val provisioners: Map<String, ProvisionerType> = LinkedHashMap(),
    val backends: Map<String, BackendType> = LinkedHashMap(),
    val functions: Map<String, Function> = LinkedHashMap()
) {
  @Suppress("MemberVisibilityCanBePrivate")
  companion object {
    private val VersionProperty = PropertyType("version", Types.String, hint = SimpleHint("VersionRange"), injectionAllowed = false)

    val Atlas: BlockType = BlockType("atlas", 0, properties = PropertyType("name", Types.String, injectionAllowed = false, required = true))
    val Module: BlockType = BlockType("module", 1, properties = *arrayOf(
        PropertyType("source", Types.String, hint = SimpleHint("Url"), required = true),
        VersionProperty,
        PropertyType("providers", Types.Object)
    ))
    val Output: BlockType = BlockType("output", 1, properties = *arrayOf(
        PropertyType("value", Types.String, hint = InterpolationHint("Any"), required = true),
        PropertyType("sensitive", Types.Boolean)
    ))

    val Variable_Type = PropertyType("type", Types.String, SimpleValueHint("string", "list", "map"), false)
    val Variable_Default = PropertyType("default", Type("String|Array|Object"))
    val Variable_Description = PropertyType("description", Types.String)
    val Variable: BlockType = BlockType("variable", 1, properties = *arrayOf(
        Variable_Type,
        Variable_Default,
        Variable_Description
    ))

    val Connection: BlockType = BlockType("connection", 0, properties = *arrayOf(
        PropertyType("type", Types.String, description = "The connection type that should be used. Valid types are \"ssh\" and \"winrm\" This defaults to \"ssh\"."),
        PropertyType("user", Types.String),
        PropertyType("password", Types.String),
        PropertyType("host", Types.String),
        PropertyType("port", Types.Number),
        PropertyType("timeout", Types.String),
        PropertyType("script_path", Types.String)
    ))
    val ConnectionPropertiesSSH: Array<out PropertyOrBlockType> = arrayOf(
        // ssh
        PropertyType("key_file", Types.String, deprecated = "Use 'private_key'"),
        PropertyType("private_key", Types.String),
        PropertyType("agent", Types.Boolean),

        // bastion ssh
        PropertyType("bastion_host", Types.String),
        PropertyType("bastion_port", Types.Number),
        PropertyType("bastion_user", Types.String),
        PropertyType("bastion_password", Types.String),
        PropertyType("bastion_private_key", Types.String),
        PropertyType("bastion_key_file", Types.String, deprecated = "Use 'bastion_private_key'")
    )
    val ConnectionPropertiesWinRM: Array<out PropertyOrBlockType> = arrayOf(
        // winrm
        PropertyType("https", Types.Boolean),
        PropertyType("insecure", Types.Boolean),
        PropertyType("cacert", Types.String)
    )

    val ResourceLifecycle: BlockType = BlockType("lifecycle", 0,
        description = "Describe to Terraform how to connect to the resource for provisioning", // TODO: Improve description
        properties = *arrayOf(
            PropertyType("create_before_destroy", Types.Boolean),
            PropertyType("prevent_destroy", Types.Boolean),
            PropertyType("ignore_changes", Types.Array, hint = TypeHint(Types.String))
        ))
    val AbstractResourceProvisioner: BlockType = BlockType("provisioner", 1, properties = *arrayOf(
        Connection
    ))

    @JvmField
    val AbstractResource: BlockType = BlockType("resource", 2, properties = *arrayOf(
        PropertyType("id", Types.String, injectionAllowed = false, description = "A unique ID for this resource", required = false),
        PropertyType("count", Types.Number),
        PropertyType("depends_on", Types.Array, hint = ReferenceHint("resource.#name", "data_source.#name")),
        PropertyType("provider", Types.String, hint = ReferenceHint("provider.#type", "provider.#alias")),
        ResourceLifecycle,
        // Also may have connection? and provisioner+ blocks
        Connection,
        AbstractResourceProvisioner
    ))
    @JvmField
    val AbstractDataSource: BlockType = BlockType("data", 2, properties = *arrayOf(
        PropertyType("id", Types.String, injectionAllowed = false, description = "A unique ID for this data source", required = false),
        PropertyType("count", Types.Number),
        PropertyType("depends_on", Types.Array, hint = ReferenceHint("resource.#name", "data_source.#name")),
        PropertyType("provider", Types.String, hint = ReferenceHint("provider.#type", "provider.#alias"))
    ))
    val AbstractProvider: BlockType = BlockType("provider", 1, required = false, properties = *arrayOf(
        PropertyType("alias", Types.String, injectionAllowed = false),
        VersionProperty
    ))
    val AbstractBackend: BlockType = BlockType("backend", 1)
    val Terraform: BlockType = BlockType("terraform", properties = *arrayOf(
        PropertyType("required_version", Types.String, injectionAllowed = false)
    ))
    val Locals: BlockType = BlockType("locals")

    val RootBlocks = listOf(Atlas, Module, Output, Variable, AbstractProvider, AbstractResource, AbstractDataSource, Terraform, Locals)
    val RootBlocksMap = RootBlocks.map { it.literal to it }.toMap()
  }

  fun getResourceType(name: String): ResourceType? {
    return resources[name]
  }

  fun getDataSourceType(name: String): DataSourceType? {
    return dataSources[name]
  }

  fun getProviderType(name: String): ProviderType? {
    return providers[name]
  }

  fun getProvisionerType(name: String): ProvisionerType? {
    return provisioners[name]
  }

  fun getBackendType(name: String): BackendType? {
    return backends[name]
  }

  fun getFunction(name: String): Function? {
    return functions[name]
  }

  fun getByFQN(fqn: String): Any? {
    val parts = fqn.split('.')
    if (parts.size < 2) return null
    val root = parts[0]
    val second = when (root) {
      "resource" -> {
        getResourceType(parts[1])
      }
      "data" -> {
        getDataSourceType(parts[1])
      }
      else -> null
    } ?: return null
    if (parts.size == 2) return second
    return find(second, parts.subList(2, parts.size))
  }

  private fun find(block: BlockType, parts: List<String>): Any? {
    if (parts.isEmpty()) return null
    val pobt = block.properties.find { it.name == parts[0] } ?: return null
    if (pobt is PropertyType) {
      return if (parts.size == 1) pobt else null
    } else if (pobt is BlockType) {
      return if (parts.size == 1) pobt else find(pobt, parts.subList(1, parts.size))
    }
    return null
  }
}