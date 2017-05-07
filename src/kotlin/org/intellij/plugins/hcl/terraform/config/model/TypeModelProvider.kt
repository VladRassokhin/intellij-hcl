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

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.boolean
import com.beust.klaxon.string
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import java.util.*

class TypeModelProvider {
  private val _model: TypeModel by lazy {
    TypeModelLoader(external).load() ?: TypeModel()
  }
  val external: Map<String, Additional> by lazy { loadExternalInformation() }
  val ignored_references: Set<String> by lazy { loadIgnoredReferences() }

  companion object {
    /**
     * Parameter 'project' is ignored for now since we have only one model.
     * When multiple different models would be supported,
     *   TypeModelProvider should become either per-project service
     *   OR properly handle project in getModel method.
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic fun getModel(project: Project) = ServiceManager.getService(TypeModelProvider::class.java)._model
  }

  private fun loadExternalInformation(): Map<String, Additional> {
    val map = HashMap<String, Additional>()
    val json = TypeModelLoader.getModelExternalInformation("external-data.json")
    if (json is JsonObject) {
      for ((fqn, obj) in json) {
        if (obj !is JsonObject) {
          TypeModelLoader.LOG.warn("In external-data.json value for '$fqn' root key is not an object")
          continue
        }
        val hintV = obj["hint"]
        val hint: Hint? = when (hintV) {
          is String -> ReferenceHint(hintV)
          is JsonArray<*> -> SimpleValueHint(*hintV.map { it.toString() }.toTypedArray())
          else -> null
        }
        val additional = Additional(fqn, obj.string("description"), hint, obj.boolean("required"))
        map.put(fqn, additional)
      }
    }
    return map
  }

  private fun loadIgnoredReferences(): Set<String> {
    try {
      val stream = TypeModelLoader.getResource("/terraform/model-external/ignored-references.list")
      if (stream == null) {
        TypeModelLoader.LOG.warn("Cannot read 'ignored-references.list': resource '/terraform/model-external/ignored-references.list' not found")
        return emptySet()
      }
      val lines = stream.bufferedReader(Charsets.UTF_8).readLines().map(String::trim).filter { !it.isEmpty() }
      return LinkedHashSet<String>(lines)
    } catch(e: Exception) {
      TypeModelLoader.LOG.warn("Cannot read 'ignored-references.list': ${e.message}")
      return emptySet()
    }
  }


  data class Additional(val name: String, val description: String? = null, val hint: Hint? = null, val required: Boolean? = null)
}