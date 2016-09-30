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
package org.intellij.plugins.hcl.navigation

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.inspection.TFNoInterpolationsAllowedInspection

class HCLQualifiedNameProvider : QualifiedNameProvider {
  override fun adjustElementToCopy(element: PsiElement?): PsiElement? {
    return element
  }

  override fun getQualifiedName(element: PsiElement?): String? {
    if (element !is HCLElement) return null

    if (element is HCLStringLiteral || element is HCLIdentifier) {
      val parent = element.parent
      if (parent is HCLBlock) {
        return getQualifiedName(parent)
      } else if (parent is HCLProperty) {
        return getQualifiedName(parent)
      }
    }
    if (element is HCLBlock) {
      return getFQN(element)
    }
    if (element is HCLProperty) {
//      TODO: Implement
//      return getFQN(element)
    }
    return null
  }

  fun getFQN(block: HCLBlock): String? {
    var elements = block.nameElements.asList()

    if (TFNoInterpolationsAllowedInspection.ResourceRootBlockSelector.accepts(block)) {
      elements = elements.drop(1)
    } else if (block.parent !is HCLFile) {
      // TODO: Implement
    }
    val sb = StringBuilder()
    elements.joinTo(sb, ".") { StringUtil.unescapeStringCharacters(HCLPsiUtil.stripQuotes(it.text)) }
    val result = sb.toString()
    if (result.isEmpty()) return null
    return result
  }

  override fun qualifiedNameToElement(fqn: String?, project: Project?): PsiElement? {
    // TODO: Implement: Search all models for resource/provider/etc
    return null
  }

  override fun insertQualifiedName(fqn: String, element: PsiElement?, editor: Editor, project: Project?) {
    EditorModificationUtil.insertStringAtCaret(editor, fqn)
  }

}
