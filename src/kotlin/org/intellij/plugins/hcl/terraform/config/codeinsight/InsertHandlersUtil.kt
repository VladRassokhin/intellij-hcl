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
package org.intellij.plugins.hcl.terraform.config.codeinsight

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.inspection.AddResourcePropertiesFix
import org.intellij.plugins.hcl.terraform.config.inspection.HCLBlockMissingPropertyInspection

object InsertHandlersUtil {
  internal fun isNextNameOnTheSameLine(element: PsiElement, document: Document): Boolean {
    val right: PsiElement?
    if (element is HCLIdentifier || element is HCLStringLiteral) {
      right = element.getNextSiblingNonWhiteSpace()
    } else if (HCLParserDefinition.IDENTIFYING_LITERALS.contains(element.node?.elementType)) {
      if (element.parent is HCLIdentifier) {
        right = element.parent.getNextSiblingNonWhiteSpace()
      } else return true
    } else return true
    if (right == null) return true
    val range = right.node.textRange
    return document.getLineNumber(range.startOffset) == document.getLineNumber(element.textRange.endOffset)
  }

  internal fun scheduleBasicCompletion(context: InsertionContext) {
    context.laterRunnable = Runnable {
      CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(context.project, context.editor)
    }
  }

  internal fun addHCLBlockRequiredProperties(file: PsiFile, editor: Editor, project: Project) {
    val block = PsiTreeUtil.getParentOfType(file.findElementAt(editor.caretModel.offset), HCLBlock::class.java)
    if (block != null) {
      addHCLBlockRequiredProperties(file, project, block)
    }
  }

  fun addHCLBlockRequiredProperties(file: PsiFile, project: Project, block: HCLBlock) {
    val inspection = HCLBlockMissingPropertyInspection()
    var changed: Boolean
    do {
      changed = false
      val holder = ProblemsHolder(InspectionManager.getInstance(project), file, true)
      val visitor = inspection.buildVisitor(holder, true, true)
      if (visitor is HCLElementVisitor) {
        visitor.visitBlock(block)
      }
      for (result in holder.results) {
        val fixes = result.fixes
        if (fixes != null && fixes.isNotEmpty()) {
          changed = true
          fixes.filterIsInstance<AddResourcePropertiesFix>().forEach { it.applyFix(project, result) }
        }
      }
    } while (changed)
  }

  internal fun addSpace(editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, " ")
  }

  internal fun addArguments(count: Int, editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, StringUtil.repeat(" \"\"", count))
  }

  internal fun addBraces(editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, " {}")
    editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
  }
}