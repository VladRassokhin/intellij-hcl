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
package org.intellij.plugins.hil.codeinsight

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import org.intellij.plugins.hcl.HCLBundle
import org.intellij.plugins.hcl.terraform.config.externalDoc.ExternalUrlReference
import org.intellij.plugins.hcl.terraform.config.externalDoc.functionSignature
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hil.psi.ILMethodCallExpression

class HILDocumentationProvider : AbstractDocumentationProvider() {

  override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): List<String>? {
    return when (element) {
      is ExternalUrlReference.MyFakePsiElement -> listOf(element.getUrl())
      else -> null
    }
  }

  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    return when (element) {
      is ExternalUrlReference.MyFakePsiElement -> {
        if (element.parent.parent is ILMethodCallExpression) {
          val functionName = element.parent.text
          val signature = functionSignature(functionName)
          if (signature != null) {
            val function = TypeModelProvider.getModel(element.project).getFunctionType(functionName)
            return when (function) {
              null -> signature
              else -> {
                val returnType = when (function.ret) {
                  Types.Array -> "list"
                  Types.Object -> "map"
                  else -> function.ret.name.toLowerCase()
                }
                "$signature -> $returnType"
              }
            }
          }
        }
        return HCLBundle.message("open.documentation.in.browser")
      }
      else -> null
    }
  }
}
