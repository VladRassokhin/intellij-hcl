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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElementVisitor
import getNameElementUnquoted
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import java.util.*

class DeprecatedElementInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val ft = holder.file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      ProgressIndicatorProvider.checkCanceled()
      block.getNameElementUnquoted(0) ?: return
      block.`object` ?: return
      val properties = ModelHelper.getBlockProperties(block);
      doCheck(block, holder, properties)
    }
  }

  private fun doCheck(block: HCLBlock, holder: ProblemsHolder, properties: Array<out PropertyOrBlockType>) {
    if (properties.isEmpty()) return
    val obj = block.`object` ?: return
    ProgressIndicatorProvider.checkCanceled()

    val candidates = ArrayList<PropertyOrBlockType>(properties.filter { it.deprecated != null })
    if (candidates.isEmpty()) return

    ProgressIndicatorProvider.checkCanceled()
    val dpn = candidates.filter { it.property != null }.map { it.name }.toHashSet()
    if (dpn.isNotEmpty()) for (hclProperty in obj.propertyList) {
      val name = hclProperty.name
      if (dpn.contains(name)) {
        holder.registerProblem(hclProperty, "Deprecated property: $name", ProblemHighlightType.LIKE_DEPRECATED)
      }
    }

    ProgressIndicatorProvider.checkCanceled()
    val dbn = candidates.filter { it.block != null }.map { it.name }.toHashSet()
    if (dbn.isNotEmpty()) for (hclBlock in obj.blockList) {
      val name = hclBlock.name
      if (dbn.contains(name)) {
        holder.registerProblem(hclBlock, "Deprecated block: $name", ProblemHighlightType.LIKE_DEPRECATED)
      }
    }
  }

}
