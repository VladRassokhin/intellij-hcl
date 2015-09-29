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
package org.intellij.plugins.hcl.terraform

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Editor
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLIdentifier
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
      else -> return // TODO: Support other block types
    }

    val project = editor.project
    if (project == null || project.isDisposed) return

    val element = file.findElementAt(context.startOffset)
    if (element !is HCLIdentifier) return

    val parent = element.parent ?: return
    if (parent is HCLProperty) {
      // ??? Do nothing
      return
    } else if (parent is HCLBlock) {
      // Count existing arguments and add missing
      val current = parent.nameElements.size()
      val expected = type.args
      if (current < expected) {
        addArguments(expected - current, editor);
      }
    } else {
      // Add arguments and braces
      val expected = type.args
      if (0 < expected) {
        addArguments(expected, editor);
      }
      addBraces(editor)
    }
    // TODO check context.completionChar before adding arguments or braces
  }

  @Suppress("UNUSED_PARAMETER")
  private fun addArguments(count: Int, editor: Editor) {
    // TODO: Implement
  }

  @Suppress("UNUSED_PARAMETER")
  private fun addBraces(editor: Editor) {
    // TODO: Implement
  }
}