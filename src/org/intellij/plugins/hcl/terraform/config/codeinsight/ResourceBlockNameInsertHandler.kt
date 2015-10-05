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
package org.intellij.plugins.hcl.terraform.config.codeinsight

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLIdentifier
import org.intellij.plugins.hcl.psi.HCLObject
import org.intellij.plugins.hcl.psi.HCLProperty
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
      "provider" -> TypeModel.AbstractProvider
      "provisioner" -> TypeModel.AbstractResourceProvisioner
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
      parent = if (p is HCLObject) p else p?.parent ?: return
    } else {
      return
    }

    if (parent is HCLProperty) {
      // ??? Do nothing
      return
    }
    var offset: Int? = null
    val current: Int
    val expected = type.args
    var addBraces = false
    if (parent is HCLBlock) {
      // Count existing arguments and add missing
      val elements = parent.nameElements
      current = elements.size() - 1
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

  private fun scheduleBasicCompletion(context: InsertionContext) {
    context.laterRunnable = object : Runnable {
      override fun run() {
        CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(context.project, context.editor)
      }
    }
  }

  private fun addSpace(editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, " ")
  }

  private fun addArguments(count: Int, editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, "${StringUtil.repeat(" \"\"", count)}")
  }

  private fun addBraces(editor: Editor) {
    EditorModificationUtil.insertStringAtCaret(editor, " {}")
    editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
  }
}