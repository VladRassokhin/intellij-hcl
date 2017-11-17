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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.navigation.HCLQualifiedNameProvider
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper

// TODO: Support overrides in separate files
class TFDuplicatedBlockPropertyInspection : TFDuplicatedInspectionBase() {

  override fun createVisitor(holder: ProblemsHolder): PsiElementVisitor {
    return MyEV(holder)
  }


  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      // TODO: Check whether it's correct to check FQN
      HCLQualifiedNameProvider.getFQN(block) ?: return
      // TODO: Support sub-blocks (based on model)
      val properties = block.`object`?.propertyList ?: return
      val model = ModelHelper.getBlockProperties(block).associateBy { it.name }
      val groupedDuplicates = properties.groupBy { it.name }
          .filterValues { it.size >= 2 }
          .filterKeys { model[it]?.property != null }
      for ((name, props) in groupedDuplicates) {
        for (prop in props) {
          holder.registerProblem(prop.nameElement, "Property '$name' declared multiple times inside one block", ProblemHighlightType.GENERIC_ERROR, *getFixes(prop, props))
        }
      }
    }
  }

  private fun getDuplicates(property: HCLProperty): List<HCLProperty>? {
    val block = property.parent?.parent as? HCLBlock ?: return null
    HCLQualifiedNameProvider.getFQN(block) ?: return null
    val properties = block.`object`?.propertyList ?: return null

    return properties.filter { it.name == property.name && it != property }
  }

  private fun getFixes(current: HCLProperty, duplicates: List<HCLProperty>): Array<LocalQuickFix> {
    val fixes = ArrayList<LocalQuickFix>()

    val first = duplicates.firstOrNull { it != current }
    first?.containingFile?.virtualFile?.let { createNavigateToDupeFix(it, first.textOffset, duplicates.size <= 2)?.let { fixes.add(it) } }
    // TODO: Support 'show all duplicates' fix
    //current.containingFile?.virtualFile?.let { createShowOtherDupesFix(it, current.textOffset, NullableFunction { param -> getDuplicates(param.parent as HCLProperty) })?.let { fixes.add(it) } }

    return fixes.toTypedArray()
  }
}