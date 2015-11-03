/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.il.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.il.codeinsight.getProvisionerResource
import org.intellij.plugins.hcl.terraform.il.codeinsight.getResource

public object ILSelectFromScopeReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is ILVariable) return PsiReference.EMPTY_ARRAY
    val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return PsiReference.EMPTY_ARRAY
    if (host !is HCLElement) return PsiReference.EMPTY_ARRAY

    val parent = element.parent
    if (parent !is ILSelectExpression) return PsiReference.EMPTY_ARRAY
    val from = parent.from
    if (from !is ILVariable) return PsiReference.EMPTY_ARRAY

    val name = element.name

    if (from.name == "var") {
      val block = host.getTerraformModule().findVariable(name)?.second
      return arrayOf(HCLBlockNameReference(element, false, block, 1))
    }
    if (from.name == "count") {
      val resource = getResource(element) ?: return PsiReference.EMPTY_ARRAY
      val property = resource.`object`?.findProperty("count")
      return arrayOf(HCLBlockPropertyReference(from, true, property))
    }
    if (from.name == "self") {
      val resource = getProvisionerResource(element) ?: return PsiReference.EMPTY_ARRAY
      val property = resource.`object`?.findProperty(name)
      return arrayOf(HCLBlockPropertyReference(element, true, property))
    }
    if (from.name == "path") {
      // TODO: Resolve some paths
      if (name == "module") {
        val file = (host as HCLElement).containingFile.originalFile
        return arrayOf(PsiReferenceBase.Immediate<ILVariable>(element, true, file.containingDirectory ?: file))
      }
    }
    if (from.name == "module") {
      val blocks = host.getTerraformModule().findModules(name)
      if (blocks.isEmpty()) return PsiReference.EMPTY_ARRAY
      return blocks.map { TerraformModuleReference(element, false, it) }.toTypedArray()
    }
    return PsiReference.EMPTY_ARRAY;
  }
}