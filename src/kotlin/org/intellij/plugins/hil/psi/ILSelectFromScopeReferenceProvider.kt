/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.intellij.plugins.hil.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hil.psi.impl.ILVariableMixin
import org.intellij.plugins.hil.psi.impl.getHCLHost

object ILSelectFromScopeReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    return getReferencesByElement(element)
  }

  fun getReferencesByElement(element: PsiElement): Array<out PsiReference> {
    if (element !is ILVariableMixin) return PsiReference.EMPTY_ARRAY
    val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return PsiReference.EMPTY_ARRAY
    if (host !is HCLElement) return PsiReference.EMPTY_ARRAY

    val parent = element.parent
    if (parent !is ILSelectExpression) return PsiReference.EMPTY_ARRAY
    val from = parent.from
    if (from !is ILVariable) return PsiReference.EMPTY_ARRAY

    if (from === element) return PsiReference.EMPTY_ARRAY

    when (from.name) {
      "var" -> {
        return arrayOf(HCLElementLazyReference(element, false) { incomplete, fake ->
          listOf(this.element.getHCLHost()?.getTerraformModule()?.findVariable(this.element.name)?.second?.nameIdentifier as HCLElement?).filterNotNull()
        })
      }
      "count" -> {
        return arrayOf(HCLElementLazyReference(from, true) { incomplete, fake ->
          listOf(
              getResource(this.element)?.`object`?.findProperty("count"),
              getDataSource(this.element)?.`object`?.findProperty("count")
          ).filterNotNull()
        })
      }
      "self" -> {
        return arrayOf(HCLElementLazyReference(element, false) { incomplete, fake ->
          val name = this.element.name
          val resource = getProvisionerResource(this.element) ?: return@HCLElementLazyReference emptyList()

          val prop = resource.`object`?.findProperty(name)
          if (prop != null) return@HCLElementLazyReference listOf(prop)
          val blocks = resource.`object`?.blockList?.filter { it.name == name }
          if (blocks != null && blocks.isNotEmpty()) return@HCLElementLazyReference blocks.map { it.nameIdentifier as HCLElement }

          if (fake) {
            val properties = ModelHelper.getResourceProperties(resource)
            for (p in properties) {
              if (p.name == name) {
                return@HCLElementLazyReference listOf(FakeHCLProperty(p.name, resource))
              }
            }
          }
          emptyList()
        })
      }
      "path" -> {
        // TODO: Resolve 'cwd' and 'root' paths
        if (element.name == "module") {
          @Suppress("USELESS_CAST")
          val file = (host as HCLElement).containingFile.originalFile
          return arrayOf(PsiReferenceBase.Immediate<ILVariable>(element, true, file.containingDirectory ?: file))
        }
      }
      "module" -> {
        return arrayOf(HCLElementLazyReference(element, false) { incomplete, fake ->
          this.element.getHCLHost()?.getTerraformModule()?.findModules(this.element.name)?.map { it.nameIdentifier as HCLElement } ?: emptyList()
        })
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
