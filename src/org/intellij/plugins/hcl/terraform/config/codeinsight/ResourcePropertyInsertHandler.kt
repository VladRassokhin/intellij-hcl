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

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.psi.PsiDocumentManager
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types

object ResourcePropertyInsertHandler : BasicInsertHandler<LookupElement>() {
  override fun handleInsert(context: InsertionContext?, item: LookupElement?) {
    if (context == null || item == null) return
    val editor = context.editor
    val project = context.project
    // Add property name - done by default

    // TODO: Check if right part already exists

    // Add equals sign
    EditorModificationUtil.insertStringAtCaret(editor, " = ")

    // Add value placeholder: "" for string; 0 for int, "${}" for string with IL, etc
    val obj = item.`object`
    if (obj is PropertyOrBlockType) {
      val property = obj.property
      if (property != null) {
        val type = property.type
        val pair = getPlaceholderValue(type)
        if (pair != null) {
          EditorModificationUtil.insertStringAtCaret(editor, pair.first)
          editor.caretModel.moveToOffset(editor.caretModel.offset - pair.second)
        }
      }
    }

    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
  }

  private fun getPlaceholderValue(type: Type): Pair<String, Int>? {
    return when (type) {
      Types.StringWithInjection -> Pair("\"\${}\"", 2)
      Types.String -> Pair("\"\"", 1)
      Types.Object -> Pair("{}", 1)
      Types.Array -> Pair("[]", 1)
      else -> null
    }
  }
}