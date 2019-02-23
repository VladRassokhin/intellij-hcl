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
package org.intellij.plugins.hil.codeinsight

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hil.HILElementTypes
import org.intellij.plugins.hil.psi.ILMethodCallExpression
import org.intellij.plugins.hil.psi.ILSelectExpression
import org.intellij.plugins.hil.psi.ILVariable

object FunctionInsertHandler : BasicInsertHandler<LookupElement>() {
  override fun handleInsert(context: InsertionContext?, item: LookupElement?) {
    if (context == null || item == null) return
    val editor = context.editor
    val file = context.file

    val project = editor.project
    if (project == null || project.isDisposed) return

    val e = file.findElementAt(context.startOffset) ?: return

    val element: PsiElement?
    if (e.node?.elementType == HILElementTypes.ID) {
      element = e.parent
    } else {
      element = e
    }
    if (element !is ILVariable) return

    val function = TypeModelProvider.getModel(project).getFunction(item.lookupString) ?: return


    var offset: Int? = null
    val current: Int
    val expected = function.arguments.size
    var addBraces = false
    var place: Int = 0


    // Probably first element in interpolation OR under ILSelectExpression
    val parent = element.parent
    if (parent is ILSelectExpression) {
      // Prohibited!
      return
    }
    if (parent is ILMethodCallExpression) {
      // Looks like function name modified
      current = parent.parameterList.parameters.size
      if (current != 0) {
        place = parent.parameterList.parameters.last().textOffset
      }
    } else {
      current = 0
      addBraces = true
    }

    // TODO check context.completionChar before adding arguments or braces

    if (context.completionChar in " (") {
      context.setAddCompletionChar(false)
    }

    if (addBraces) {
      addBraces(editor, expected)
      editor.caretModel.moveToOffset(editor.caretModel.offset + 1)
      scheduleBasicCompletion(context)
    } else if (current < expected) {
      // TODO: Add some arguments
      //      offset = editor.caretModel.offset + 2
      //      addArguments(expected, editor, place)
      //      scheduleBasicCompletion(context)
    }
    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
    if (offset != null) {
      editor.caretModel.moveToOffset(offset)
    }
  }


  private fun scheduleBasicCompletion(context: InsertionContext) {
    context.laterRunnable = object : Runnable {
      override fun run() {
        if (context.project.isDisposed || context.editor.isDisposed) return
        CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(context.project, context.editor)
      }
    }
  }

  private fun addArguments(count: Int, editor: Editor, place: Int) {
    val offset = editor.caretModel.offset
    editor.caretModel.moveToOffset(place)
    EditorModificationUtil.insertStringAtCaret(editor, "(${StringUtil.repeat(", ", count)})")
    editor.caretModel.moveToOffset(offset)
  }

  private fun addBraces(editor: Editor, expected: Int) {
    EditorModificationUtil.insertStringAtCaret(editor, "(${StringUtil.join((1..expected).map { "" }, ", ")})", false, false)
    //    EditorModificationUtil.insertStringAtCaret(editor, " {}")
    //    editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
  }

}
