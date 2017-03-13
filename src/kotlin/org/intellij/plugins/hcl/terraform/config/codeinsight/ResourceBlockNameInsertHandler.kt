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

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.model.TypeModel

object ResourceBlockNameInsertHandler : BasicInsertHandler<LookupElement>() {
  override fun handleInsert(context: InsertionContext?, item: LookupElement?) {
    if (context == null || item == null) return
    val editor = context.editor
    val file = context.file
    val type = when (item.lookupString) {
      "atlas" -> TypeModel.Atlas
      "module" -> TypeModel.Module
      "output" -> TypeModel.Output
      "variable" -> TypeModel.Variable
      "resource" -> TypeModel.AbstractResource
      "data" -> TypeModel.AbstractDataSource
      "provider" -> TypeModel.AbstractProvider
      "provisioner" -> TypeModel.AbstractResourceProvisioner
      "connection" -> TypeModel.Connection
      "lifecycle" -> TypeModel.ResourceLifecycle
      else -> return // TODO: Support other block types
    }

    val project = editor.project
    if (project == null || project.isDisposed) return

    val element = file.findElementAt(context.startOffset) ?: return
    val parent: PsiElement

    if (element is HCLIdentifier) {
      parent = element.parent ?: return
    } else if (element.node?.elementType == HCLElementTypes.ID) {
      val p = element.parent
      parent = p as? HCLObject ?: p?.parent ?: return
    } else {
      return
    }

    if (parent is HCLProperty) {
      // ??? Do nothing
      return
    }
    if (item.lookupString in TypeModel.RootBlocksMap.keys && parent.parent !is PsiFile) {
      return
    }
    var offset: Int? = null
    val current: Int
    val expected = type.args
    var addBraces = false
    if (parent is HCLBlock && isNextNameOnTheSameLine(element, context.document)) {
      // Count existing arguments and add missing
      val elements = parent.nameElements
      current = elements.size - 1
      // Locate caret to latest argument
      val last = elements.last()
      // TODO: Move caret to last argument properly
      editor.caretModel.moveToOffset(last.textRange.endOffset)
    } else {
      // Add arguments and braces
      current = 0
      addBraces = true
    }
    // TODO check context.completionChar before adding arguments or braces

    if (current < expected) {
      offset = editor.caretModel.offset + 2
      addArguments(expected, editor)
      scheduleBasicCompletion(context)
    }
    if (addBraces) {
      addBraces(editor)
    }

    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
    if (offset != null) {
      editor.caretModel.moveToOffset(offset)
    }
  }

  private fun isNextNameOnTheSameLine(element: PsiElement, document: Document): Boolean {
    val right: PsiElement?
    if (element is HCLIdentifier) {
      right = element.getNextSiblingNonWhiteSpace()
    } else if (element.node?.elementType == HCLElementTypes.ID) {
      if (element.parent is HCLIdentifier) {
        right = element.parent.getNextSiblingNonWhiteSpace()
      } else return true
    } else return true
    if (right == null) return true
    val range = right.node.textRange
    return document.getLineNumber(range.startOffset) == document.getLineNumber(element.textRange.endOffset)
  }

  private fun scheduleBasicCompletion(context: InsertionContext) {
    context.laterRunnable = Runnable {
      CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(context.project, context.editor)
    }
  }

  private fun addSpace(editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, " ")
  }

  private fun addArguments(count: Int, editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, StringUtil.repeat(" \"\"", count))
  }

  private fun addBraces(editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, " {}")
    editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
  }
}
