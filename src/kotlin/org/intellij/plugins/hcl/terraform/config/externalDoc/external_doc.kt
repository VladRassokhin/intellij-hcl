/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.config.externalDoc

import com.beust.klaxon.*
import org.intellij.plugins.hcl.terraform.config.model.TypeModelLoader
import java.net.URI

const val BASE_URL = "https://www.terraform.io/docs"

const val FUNCTIONS_URL = "$BASE_URL/configuration/interpolation.html"

private class Data(
  val functionFragments: Map<String, String>,
  val functionSignatures: Map<String, String>,
  val keepProviderPrefix: Set<String>,
  val keywords: Map<String, String>,
  val paths: Map<String, String>
)

private fun pairsToMap(obj: JsonObject, fieldName: String): MutableMap<String, String> {
  return obj.array<JsonArray<String>>(fieldName)!!.associate { it[0] to it[1] } as MutableMap<String, String>
}

private val data: Data by lazy {
  val docData = TypeModelLoader.getResourceJson("${TypeModelLoader.ModelResourcesPrefix}-external/doc-data.json") as JsonObject
  val docDataGenerated = TypeModelLoader.getResourceJson("${TypeModelLoader.ModelResourcesPrefix}/doc-data-generated.json") as JsonObject

  val functionFragments = mutableMapOf<String,String>()
  val functionSignatures = mutableMapOf<String,String>()

  for ( f in docDataGenerated.array<JsonObject>("functions")!!) {
    val name = f.string("name")!!
    functionFragments[name] = f.string("fragment")!!
    functionSignatures[name] = f.string("signature")!!
  }

  val paths = pairsToMap(docData, "paths")
  paths.putAll(pairsToMap(docDataGenerated, "paths"))

  Data(
      functionFragments = functionFragments,
      functionSignatures = functionSignatures,
      keepProviderPrefix = docData.array<String>("keepProviderPrefix")!!.toSet(),
      keywords = pairsToMap(docData, "keywords"),
      paths = paths
  )
}

fun urlForBackendTypeDoc(type: String): String {
  return "$BASE_URL/backends/types/${data.paths["backend.$type"] ?: type}.html"
}

fun urlForProviderTypeDoc(type: String): String {
  return "$BASE_URL/providers/${data.paths["provider.$type"] ?: type}/"
}

fun urlForProvisionerTypeDoc(type: String): String {
  return "$BASE_URL/provisioners/$type.html"
}

fun urlForKeywordDoc(keyword: String): String? {
  val path = data.keywords[keyword]
  return if (path == null) null else "$BASE_URL/$path"
}

fun functionFragment(name: String): String? {
  return data.functionFragments[name]
}

fun functionSignature(name: String): String? {
  return data.functionSignatures[name]
}

fun urlForFunctionDoc(name: String): String {
  return "$FUNCTIONS_URL#${functionFragment(name)}"
}

private fun urlForDataSourceOrResourceDoc(providerName: String, dataOrResource: String, type: String): String {
  val base = "${urlForProviderTypeDoc(providerName)}${dataOrResource[0]}/"
  val path = data.paths["$dataOrResource.$type"]
  return when {
    path != null -> URI("$base$path.html").normalize().toString()
    data.keepProviderPrefix.contains(providerName) -> "$base$type.html"
    else -> "$base${type.substringAfter('_')}.html"
  }
}

fun urlForDataSourceTypeDoc(providerType: String, type: String): String {
  return urlForDataSourceOrResourceDoc(providerType, "data", type)
}

fun urlForResourceTypeDoc(providerType: String, type: String): String {
  return urlForDataSourceOrResourceDoc(providerType, "resource", type)
}
