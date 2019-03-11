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
package org.intellij.plugins.hcl.terraform.config

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

object DotTerraformUtil {

  fun findTerraformDir(searchFrom: PsiDirectory) = CachedValuesManager.getCachedValue(searchFrom, TerraformDirCachedValueProvider(searchFrom))

  class TerraformDirCachedValueProvider(private val searchFrom: PsiDirectory) : CachedValueProvider<VirtualFile?> {
    override fun compute(): CachedValueProvider.Result<VirtualFile?>? {
      // Includes as dependency items the chain of PsiDirectory objects that were searched so that a .terraform dir added higher up the
      // chain will invalidate the cached result.
      //
      // PsiDirectory objects become "out of date" more than strictly necessary for our purpose here.  The only alternative I've thought of
      // is depending on the VirtualFile for each dir but those do not become out of date when a file is added (!?) so cannot help here.

      val projectfileIndex = ProjectFileIndex.SERVICE.getInstance(searchFrom.project)
      val dependencies = mutableListOf<Any>()
      var lookIn: PsiDirectory? = searchFrom

      while (lookIn != null && projectfileIndex.isInContent(lookIn.virtualFile)) {
        dependencies.add(lookIn)
        val found = lookIn.findSubdirectory(".terraform")
        if (found != null) {
          dependencies.add(found)
          return CachedValueProvider.Result(found.virtualFile, *dependencies.toTypedArray())
        }
        lookIn = lookIn.parent
      }
      return CachedValueProvider.Result(null, *dependencies.toTypedArray())
    }
  }
}
