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
    val resources: List<ResourceType> = arrayListOf(),
    val dataSources: List<DataSourceType> = arrayListOf(),
    val providers: List<ProviderType> = arrayListOf(),
    val provisioners: List<ProvisionerType> = arrayListOf(),
    val backends: MutableList<BackendType> = arrayListOf(),
    val functions: List<Function> = arrayListOf()
) {
  companion object {
    val Atlas: BlockType = BlockType("atlas", 0, properties = PropertyType("name", Types.String, injectionAllowed = false, required = true).toPOBT())
    val Module: BlockType = BlockType("module", 1, properties = PropertyType("source", Types.String, hint = SimpleHint("Url"), required = true).toPOBT())
    val Output: BlockType = BlockType("output", 1, properties = *arrayOf(
        PropertyType("value", Types.String, hint = InterpolationHint("Any"), required = true).toPOBT(),
        PropertyType("sensitive", Types.Boolean).toPOBT()
    ))

    val Variable_Type = PropertyType("type", Types.String, SimpleValueHint("string", "list", "map"), false)
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
    )
    val ConnectionPropertiesWinRM: Array<out PropertyOrBlockType> = arrayOf(
        // winrm
        PropertyType("https", Types.Boolean).toPOBT(),
        PropertyType("insecure", Types.Boolean).toPOBT(),
        PropertyType("cacert", Types.String).toPOBT()
    )

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
        PropertyType("depends_on", Types.Array, hint = ReferenceHint("resource.#name", "data_source.#name")).toPOBT(),
        PropertyType("provider", Types.String, hint = ReferenceHint("provider.#type", "provider.#alias")).toPOBT(),
        ResourceLifecycle.toPOBT(),
        // Also may have connection? and provisioner+ blocks
        Connection.toPOBT(),
        AbstractResourceProvisioner.toPOBT()
    ))
    @JvmField val AbstractDataSource: BlockType = BlockType("data", 2, properties = *arrayOf(
        PropertyType("id", Types.String, injectionAllowed = false, description = "A unique ID for this data source", required = false).toPOBT(),
        PropertyType("count", Types.Number).toPOBT(),
        PropertyType("depends_on", Types.Array, hint = ReferenceHint("resource.#name", "data_source.#name")).toPOBT(),
        PropertyType("provider", Types.String, hint = ReferenceHint("provider.#type", "provider.#alias")).toPOBT()
    ))
    val AbstractProvider: BlockType = BlockType("provider", 1, required = false, properties = *arrayOf(
        PropertyType("alias", Types.String, injectionAllowed = false).toPOBT())
    )
    val AbstractBackend: BlockType = BlockType("backend", 1)
    val Terraform: BlockType = BlockType("terraform", properties = *arrayOf(
        PropertyType("required_version", Types.String, injectionAllowed = false).toPOBT()
    ))

    val RootBlocks = listOf(Atlas, Module, Output, Variable, AbstractProvider, AbstractResource, AbstractDataSource, Terraform)
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

  fun getBackendType(name: String): BackendType? {
    return backends.firstOrNull { it.type == name }
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
    if (pobt.property != null) {
      return if (parts.size == 1) pobt else null
    } else if (pobt.block != null) {
      return if (parts.size == 1) pobt else find(pobt.block, parts.subList(1, parts.size))
    }
    return null
  }
}