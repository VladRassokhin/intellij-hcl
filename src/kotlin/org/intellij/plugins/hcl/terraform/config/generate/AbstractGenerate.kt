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
package org.intellij.plugins.hcl.terraform.config.generate

import com.intellij.codeInsight.CodeInsightUtilBase
import com.intellij.codeInsight.actions.SimpleCodeInsightAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.*
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.HCLFile
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionContributor
import org.intellij.plugins.hcl.terraform.config.inspection.HCLBlockMissingPropertyInspection
import java.util.*


abstract class AbstractGenerate() : SimpleCodeInsightAction() {
  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    if (!CodeInsightUtilBase.prepareEditorForWrite(editor)) return
    val offset = editor.caretModel.currentCaret.offset
    val marker = editor.document.createRangeMarker(offset, offset)
    marker.isGreedyToLeft = true
    marker.isGreedyToRight = true
    TemplateManager.getInstance(project).startTemplate(editor, template, false, null, object : TemplateEditingAdapter() {
      override fun templateFinished(template: Template?, brokenOff: Boolean) {
        if (!brokenOff) {
          val element = file.findElementAt(marker.startOffset)
          val block = PsiTreeUtil.getParentOfType(element, HCLBlock::class.java, false)
          if (block != null) {
            if (TextRange(marker.startOffset, marker.endOffset).contains(block.textOffset)) {
              // It's out new block
              // TODO: Invoke add properties quick fix
              val inspection = HCLBlockMissingPropertyInspection()
              val holder = ProblemsHolder(InspectionManager.getInstance(project), file, true)
              val visitor = inspection.buildVisitor(holder, true)
              if (visitor is HCLElementVisitor) {
                visitor.visitBlock(block);
              }
              for (result in holder.results) {
                result.fixes?.forEach { it.applyFix(project, result) }
              }
            }
          }
        }
      }
    })
  }

  abstract val template: Template

  override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
    return file is HCLFile && file.fileType == TerraformFileType
  }

  companion object {
    val InvokeCompletionExpression: Expression = object : Expression() {
      override fun calculateQuickResult(context: ExpressionContext?): Result? {
        return null //calculateResult(context)
      }

      override fun calculateResult(context: ExpressionContext?): Result? {
        val lookupItems = calculateLookupItems(context)
        if (lookupItems == null || lookupItems.size == 0) return TextResult("")

        return TextResult(lookupItems[0].lookupString)
      }

      override fun calculateLookupItems(context: ExpressionContext?): Array<out LookupElement>? {
        if (context == null) return null
        val editor = context.editor ?: return null
        val file = PsiDocumentManager.getInstance(context.project).getPsiFile(editor.document) ?: return null
        val element = file.findElementAt(context.startOffset) ?: return null
        val consumer = ArrayList<LookupElementBuilder>()
        TerraformConfigCompletionContributor.BlockTypeOrNameCompletionProvider.doCompletion(element, consumer)
        consumer.sortBy { it.lookupString }
        return consumer.toTypedArray()
      }
    }
  }
}
