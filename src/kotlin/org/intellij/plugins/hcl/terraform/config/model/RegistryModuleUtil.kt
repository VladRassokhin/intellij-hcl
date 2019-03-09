/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import java.util.regex.Pattern

// Based on Terraform sources
object RegistryModuleUtil {
  fun parseRegistryModule(source: String): RegistryModule? {
    val (host, rest) = parseFriendlyHost(source)
    if (host != null) {
      if (host.toLowerCase() in listOf("github.com", "bitbucket.org")) return null
      if (!isValidHost(host)) return null
    }

    val m = ModuleSourceRE.matcher(rest)
    if (!m.find()) return null

    return RegistryModule(host, m.group(1), m.group(2), m.group(3), m.group(4))
  }

  private fun isValidHost(given: String): Boolean {
    val split = given.split(':', limit = 2)
    val host = split[0]
    if (split.size == 2) {
      val port = split[1].toIntOrNull() ?: return false
      if (port > 65535) return false
    }
    if (host.isEmpty()) return false

    val parts = host.split('.')
    for (part in parts) {
      if (part.isEmpty()) return false
      if (part.startsWith("xn--")) return false
    }
    try {
      java.net.IDN.toASCII(host)
    } catch (e: Exception) {
      return false
    }
    return true
  }

  private fun parseFriendlyHost(source: String): Pair<String?, String> {
    val parts = source.split('/', limit = 2)
    if (parts.isEmpty()) return null to ""
    val host = parts[0]
    return if (host.matches(HostRE)) {
      host to (parts.getOrNull(1) ?: "")
    } else {
      null to source
    }
  }

  private val nameSubRe = "[0-9A-Za-z](?:[0-9A-Za-z-_]{0,62}[0-9A-Za-z])?"
  private val providerSubRe = "[0-9a-z]{1,64}"
  private val ModuleSourceRE = Pattern.compile(String.format("^(%s)\\/(%s)\\/(%s)(?:\\/\\/(.*))?$", nameSubRe, nameSubRe, providerSubRe))


  private val urlLabelEndSubRe = "[0-9A-Za-z]"
  private val urlLabelMidSubRe = "[0-9A-Za-z-]"
  private val urlLabelUnicodeSubRe = "[^[:ascii:]]"
  private val hostLabelSubRe = "" +
      // Match valid initial char, or unicode char
      "(?:" + urlLabelEndSubRe + "|" + urlLabelUnicodeSubRe + ")" +
      // Optionally, match 0 to 61 valid URL or Unicode chars,
      // followed by one valid end char or unicode char
      "(?:" +
      "(?:" + urlLabelMidSubRe + "|" + urlLabelUnicodeSubRe + "){0,61}" +
      "(?:" + urlLabelEndSubRe + "|" + urlLabelUnicodeSubRe + ")" +
      ")?"
  private val hostSubRe = "$hostLabelSubRe(?:\\.$hostLabelSubRe)+(?::\\d+)?"
  private val HostRE: Regex = "^$hostSubRe$".toRegex()

  // registry/regsrc/module.go:Module
  data class RegistryModule(val host: String?, val namespace: String, val name: String, val provider: String, val submodule: String?) {
    override fun toString(): String {
      val prefix = if (host != null) "$host/" else ""
      return formatWithPrefix(prefix, true)
    }

    private fun formatWithPrefix(hostPrefix: String, preserveCase: Boolean): String {
      val suffix = if (submodule != null) "//$submodule" else ""
      val str = "$hostPrefix$namespace/$name/$provider$suffix"
      if (!preserveCase) return str.toLowerCase()
      return str
    }
  }
}