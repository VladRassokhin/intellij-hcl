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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper

class TFDefaultPropertyInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return if (holder.file.fileType == TerraformFileType) MyEV(holder) else super.buildVisitor(holder, isOnTheFly)
    }

    inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
        override fun visitBlock(block: HCLBlock) {
            val properties = block.`object`?.propertyList ?: return
            val model = ModelHelper.getBlockProperties(block).associateBy { it.name }
            for (p in properties) {
                val name = p.name
                val defaultValue = model[name]?.defaultValue ?: continue
                val value = p.value ?: continue
                if (isEquals(value, defaultValue)) {
                    holder.registerProblem(p, "'$name' is set to its default value", ProblemHighlightType.LIKE_UNUSED_SYMBOL, DeletePropertyFix)
                }
            }
        }

        private fun isEquals(value: HCLValue, defaultValue: Any): Boolean {
            return when (value) {
                is HCLBooleanLiteral -> value.value == defaultValue
                is HCLNumberLiteral -> value.value == defaultValue
                is HCLStringLiteral -> value.value == defaultValue
                else -> false
            }
        }
    }

    private object DeletePropertyFix : LocalQuickFixBase("Delete property"), LowPriorityAction {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val property = descriptor.psiElement as? HCLProperty ?: return
            ApplicationManager.getApplication().runWriteAction { property.delete() }
        }
    }
}
