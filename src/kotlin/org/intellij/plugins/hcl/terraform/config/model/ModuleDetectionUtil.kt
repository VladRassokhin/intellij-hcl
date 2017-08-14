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

import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.utils.URIBuilder
import java.net.URI
import java.net.URISyntaxException

object ModuleDetectionUtil {
  fun computeModuleStorageName(name: String, source: String): String {
    // TODO: Improve path calculation
    val path = listOf(name).joinToString(".") { it }
    val md5 = DigestUtils.md5Hex("root.$path-$source")!!
    return md5
  }

  fun getModuleSourceAdditionalPath(source: String): String? {
    val detected = detect(source) ?: return null
    val (_, unforced) = getForcedGetter(detected)
    val (_, subdir) = getSourceDirSubdir(unforced)
    return if (subdir.isNullOrEmpty()) null else subdir
  }

  interface Detector {
    fun detect(source: String): String?
  }

  object GitHubDetector : Detector {
    override fun detect(source: String): String? {
      if (source.startsWith("github.com/")) {
        return detectHTTP(source)
      }
      if (source.startsWith("git@github.com:")) {
        return detectSSH(source)
      }
      return null
    }

    private fun detectHTTP(source: String): String? {
      val parts = source.split('/')
      if (parts.size < 3) return null

      val urlStr = "https://" + parts.subList(0, 3).joinToString("/")
      val uri: URIBuilder
      try {
        uri = URIBuilder(urlStr)
      } catch(e: URISyntaxException) {
        return null
      }
      if (!uri.path.endsWith(".git")) uri.path += ".git"

      if (parts.size > 3) {
        uri.path += "//" + parts.subList(3, parts.lastIndex + 1).joinToString("/")
      }

      return "git::" + uri.toString()
    }

    private fun detectSSH(source: String): String? {
      val idx = source.indexOf(":")
      var qidx = source.indexOf("?")
      if (qidx == -1) qidx = source.length

      val u = URIBuilder()
      u.scheme = "ssh"
      u.userInfo = "git"
      u.host = "github.com"
      u.path = source.substring(idx + 1, qidx)
      if (qidx < source.length) {
        u.setCustomQuery(source.substring(qidx))
      }
      return "git::" + u.toString()
    }
  }

  object BitBucketDetector : Detector {
    override fun detect(source: String): String? {
      if (source.startsWith("bitbucket.org/")) {
        return detectHTTP(source)
      }
      return null
    }

    private fun detectHTTP(source: String): String? {
      // Simple fast git-only detector
      try {
        return "git::" + URI("https://" + source).toString()
      } catch(e: URISyntaxException) {
        return null
      }
    }
  }


  val Detectors = listOf<Detector>(GitHubDetector, BitBucketDetector)

  fun getForcedGetter(src: String): Pair<String, String> {
    val result = "^([A-Za-z0-9]+)::(.+)\$".toRegex().find(src)
    if (result != null) {
      return result.groupValues[1] to result.groupValues[2]
    }
    return "" to src
  }

  fun getSourceDirSubdir(source: String): Pair<String, String> {
    var src = source
    var offset: Int = 0

    // Calculate an offset to avoid accidentally marking the scheme
    // as the dir.
    var idx = src.indexOf("://", offset)
    if (idx > -1) offset = idx + 3

    // First see if we even have an explicit subdir
    idx = src.indexOf("//", offset)
    if (idx == -1) return src to ""

    var subdir = src.substring(idx + 2)
    src = src.substring(0, idx)

    idx = subdir.indexOf('?')
    if (idx > -1) {
      val query = subdir.substring(idx)
      subdir = subdir.substring(0, idx)
      src += query
    }

    return src to subdir
  }


  @Throws(URISyntaxException::class)
  fun detect(src: String): String? {
    var (getForce, getSrc) = getForcedGetter(src)

    // Separate out the subdir if there is one, we don't pass that to detect
    val pair = getSourceDirSubdir(getSrc)
    getSrc = pair.first
    var subDir = pair.second
    try {
      if (!URI(getSrc).scheme.isNullOrEmpty()) return src
    } catch(ignored: URISyntaxException) {
    }
    for (detector in Detectors) {
      var result: String = detector.detect(getSrc) ?: continue
      var detectForce: String = ""
      if (true) {
        val pair = getForcedGetter(result)
        detectForce = pair.first
        result = pair.second
      }

      var detectSubdir: String = ""
      if (true) {
        val pair = getSourceDirSubdir(result)
        result = pair.first
        detectSubdir = pair.second
      }

      // If we have a subdir from the detection, then prepend it to our
      // requested subdir.
      if (detectSubdir != "") {
        if (subDir != "") {
          subDir = detectSubdir + "/" + subDir
        } else {
          subDir = detectSubdir
        }
      }
      if (subDir != "") {
        val builder = URIBuilder(result)
        builder.path += "//" + subDir
        result = builder.toString()
      }
      // Preserve the forced getter if it exists. We try to use the
      // original set force first, followed by any force set by the
      // detector.
      if (getForce != "") {
        result = String.format("%s::%s", getForce, result)
      } else if (detectForce != "") {
        result = String.format("%s::%s", detectForce, result)
      }
      return result
    }
    return null
  }
}