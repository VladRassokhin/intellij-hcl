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
import com.intellij.psi.util.PsiTreeUtil
import getNameElementUnquoted
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.HCLFile
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.model.TypeModel

class UnknownBlockTypeInspection : LocalInspectionTool() {

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
            val type = block.getNameElementUnquoted(0) ?: return
            doCheck(block, holder, type)
        }
    }

    private fun doCheck(block: HCLBlock, holder: ProblemsHolder, type: String) {
        if (type.isEmpty()) return
        // It could be root block OR block inside Object.
        // Object could be value of some property or right part of other object
        val parent = PsiTreeUtil.getParentOfType(block, HCLBlock::class.java, HCLProperty::class.java, HCLFile::class.java) ?: return
        ProgressIndicatorProvider.checkCanceled()
        if (parent is HCLFile) {
            if (TypeModel.RootBlocks.any { it.literal == type }) return
            holder.registerProblem(block.nameElements.first(), "Unknown block type $type", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        } else if (parent is HCLBlock) {
            parent.getNameElementUnquoted(0) ?: return
            parent.`object` ?: return
            val properties = ModelHelper.getBlockProperties(parent);
            // TODO: (?) For some reason single name block could be represented as 'property' in model
            if (properties.any { it.block != null && it.name == type }) return
            holder.registerProblem(block.nameElements.first(), "Unknown block type $type", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        } else if (parent is HCLProperty) {
            // TODO: Add some logic
        } else return
        // TODO: Add 'Register as known block type' quick fix
    }
}