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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.NullableFunction
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.model.getProviderFQName
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns

class TFDuplicatedProviderInspection : TFDuplicatedInspectionBase() {

  override fun createVisitor(holder: ProblemsHolder): PsiElementVisitor {
    return MyEV(holder)
  }


  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      val duplicates = getDuplicates(block) ?: return
      val fqn = block.getProviderFQName() ?: return
      holder.registerProblem(block, "Provider '$fqn' declared multiple times", ProblemHighlightType.GENERIC_ERROR, *getFixes(fqn != block.getNameElementUnquoted(1), block, duplicates))
    }
  }

  private fun getDuplicates(block: HCLBlock): List<HCLBlock>? {
    if (!TerraformPatterns.ProviderRootBlock.accepts(block)) return null
    if (TerraformPatterns.ConfigOverrideFile.accepts(block.containingFile)) return null

    val module = block.getTerraformModule()

    val fqn = block.getProviderFQName() ?: return null

    val same = module.getDefinedProviders().filter { it.second == fqn && !TerraformPatterns.ConfigOverrideFile.accepts(it.first.containingFile) }
    if (same.isEmpty()) return null
    if (same.size == 1) {
      assert(same.first().first == block)
      return null
    }
    return same.map { it.first }
  }

  private fun getFixes(aliased: Boolean, block: HCLBlock, duplicates: List<HCLBlock>): Array<LocalQuickFix> {
    val fixes = ArrayList<LocalQuickFix>()

    val first = duplicates.firstOrNull { it != block }
    first?.containingFile?.virtualFile?.let { createNavigateToDupeFix(it, first.textOffset, duplicates.size <= 2)?.let { fixes.add(it) } }
    block.containingFile?.virtualFile?.let { createShowOtherDupesFix(it, block.textOffset, NullableFunction { param -> getDuplicates(param as HCLBlock) })?.let { fixes.add(it) } }

    if (false) {
      // TODO: Implement fixes
      if (aliased) {
        fixes.add(ChangeAliasFix)
      } else {
        fixes.add(AddAliasFix)
      }
    }

    return fixes.toTypedArray()
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

