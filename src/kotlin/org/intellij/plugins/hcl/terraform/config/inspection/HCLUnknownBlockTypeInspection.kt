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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionContributor
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns

class HCLUnknownBlockTypeInspection : LocalInspectionTool() {

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
            val properties = ModelHelper.getBlockProperties(parent)
            // TODO: (?) For some reason single name block could be represented as 'property' in model
            if (properties.any { it.block != null && it.name == type }) return

            // Check for non-closed root block (issue #93)
            if (TerraformPatterns.RootBlock.accepts(parent) && TerraformConfigCompletionContributor.ROOT_BLOCK_KEYWORDS.contains(type)) {
                holder.registerProblem(block.nameElements.first(), "Missing closing brace on previous line", ProblemHighlightType.GENERIC_ERROR, AddClosingBraceFix(block.nameElements.first()))
                return
            }

            holder.registerProblem(block.nameElements.first(), "Unknown block type $type", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        } else if (parent is HCLProperty) {
            // TODO: Add some logic
        } else return
        // TODO: Add 'Register as known block type' quick fix
    }
}

class AddClosingBraceFix(before: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(before) {
    override fun getText(): String {
        return "Add closing braces before element"
    }

    override fun getFamilyName(): String {
        return text
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return
        val element = startElement
        object : WriteCommandAction<Any>(project) {
            override fun run(result: Result<Any>) {
                CodeStyleManager.getInstance(project).performActionWithFormatterDisabled {
                    if (editor != null) {
                        editor.document.insertString(element.node.startOffset, "}\n")
                    } else {
                        element.parent.addBefore(HCLElementGenerator(project).createObject("").lastChild, element)
                        element.parent.addBefore(PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n"),element)
                        file.subtreeChanged()
                    }
                }
            }
        }.execute()
    }
}