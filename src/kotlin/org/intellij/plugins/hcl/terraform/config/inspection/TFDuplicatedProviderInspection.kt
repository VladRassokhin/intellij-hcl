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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.getProviderFQName
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.config.psi.TerraformReferenceContributor

class TFDuplicatedProviderInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (file.fileType != TerraformFileType || file.name.endsWith("." + TerraformFileType.TFVARS_EXTENSION)) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  companion object {
    val ProviderRootBlockSelector: PsiElementPattern.Capture<HCLBlock> =
        psiElement(HCLBlock::class.java)
            .withParent(TerraformReferenceContributor.TerraformConfigFile)
            .with(object : PatternCondition<HCLBlock?>("HCLBlock(provider)") {
              override fun accepts(t: HCLBlock, context: ProcessingContext?): Boolean {
                return t.getNameElementUnquoted(0) == "provider"
              }
            })
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      if (!ProviderRootBlockSelector.accepts(block)) return

      val module = block.getTerraformModule()

      val fqn = getProviderFQName(block) ?: return

      val same = module.getDefinedProviders().filter { it.second == fqn }
      if (same.size == 0) return
      if (same.size == 1) {
        assert(same.first().first == block)
        return
      }
      holder.registerProblem(block, "Provider '$fqn' declared multiple times", ProblemHighlightType.GENERIC_ERROR, *getFixes(fqn != block.getNameElementUnquoted(1)))
    }
  }

  private fun getFixes(aliased: Boolean): Array<LocalQuickFix> {
    return emptyArray()
    return arrayOf(
        if (aliased) ChangeAliasFix
        else AddAliasFix
    )
  }

  private object AddAliasFix : LocalQuickFixBase("Add provider alias") {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      TODO("Implement")
    }
  }

  private object ChangeAliasFix : LocalQuickFixBase("Change provider alias") {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      TODO("Implement")
    }
  }
}

