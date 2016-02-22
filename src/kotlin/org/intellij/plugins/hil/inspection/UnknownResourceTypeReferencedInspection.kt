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
package org.intellij.plugins.hil.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElementVisitor
import getNameElementUnquoted
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hil.codeinsight.HILCompletionContributor
import org.intellij.plugins.hil.psi.ILElementVisitor
import org.intellij.plugins.hil.psi.ILSelectExpression
import org.intellij.plugins.hil.psi.ILVariable

class UnknownResourceTypeReferencedInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = InjectedLanguageManager.getInstance(holder.project).getTopLevelFile(holder.file)
    val ft = file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  inner class MyEV(val holder: ProblemsHolder) : ILElementVisitor() {
    override fun visitILVariable(element: ILVariable) {
      ProgressIndicatorProvider.checkCanceled()
      val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return
      if (host !is HCLElement) return
      val parent = element.parent
      if (parent !is ILSelectExpression) return
      if (parent.from !== element) return

      val name = element.name

      if (HILCompletionContributor.SCOPES.contains(name)) return
      if (isExistingResourceType(element, host)) return

      holder.registerProblem(element, "Unknown resource type", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }
  }

}


fun isExistingResourceType(element: ILVariable, host: HCLElement): Boolean {
  val name = element.name
  val module = host.getTerraformModule()
  val resources = module.getDeclaredResources()
  return resources.any { name == it.getNameElementUnquoted(1) }
}
