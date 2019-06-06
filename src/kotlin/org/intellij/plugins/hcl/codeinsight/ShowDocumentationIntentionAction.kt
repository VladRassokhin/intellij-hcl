/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.codeinsight

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLIdentifier
import org.intellij.plugins.hcl.psi.HCLStringLiteral

class ShowDocumentationIntentionAction : BaseIntentionAction(), LowPriorityAction {
  private val supportedIdentifiers = arrayOf("resource", "data")

  private fun url(identifier: String, element: String): String {
    val resource = identifier[0]
    val (provider, id) = element.trim('"').split("_", limit = 2)
    return "https://www.terraform.io/docs/providers/$provider/$resource/$id.html"
  }

  override fun getText() = "Show Terraform documentation"

  override fun getFamilyName() = text

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = findElements(editor, file) != null

  override fun startInWriteAction() = false

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    val (identifier, value) = findElements(editor, file) ?: return
    BrowserUtil.browse(url(identifier, value))
  }

  private fun findElements(editor: Editor?, file: PsiFile?): Pair<String, String>? {
    if (editor == null || file == null) {
      return null
    }

    val element = PsiTreeUtil.findElementOfClassAtOffset(file, editor.caretModel.offset, HCLStringLiteral::class.java, false) ?: return null
    val identifier = PsiTreeUtil.getPrevSiblingOfType(element, HCLIdentifier::class.java) ?: return null

    return when {
      element.parent is HCLBlock && identifier.text in supportedIdentifiers -> identifier.text to element.text
      else -> null
    }
  }
}