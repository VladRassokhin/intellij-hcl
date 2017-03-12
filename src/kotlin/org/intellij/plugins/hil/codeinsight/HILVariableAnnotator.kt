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

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import org.intellij.plugins.hcl.psi.HCLFile
import org.intellij.plugins.hil.HILSyntaxHighlighterFactory
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.ILSelectExpression
import org.intellij.plugins.hil.psi.ILVariable
import org.intellij.plugins.hil.psi.getGoodLeftElement
import org.intellij.plugins.hil.psi.impl.getHCLHost

class HILVariableAnnotator : Annotator {
  companion object {
    private val DEBUG = ApplicationManager.getApplication().isUnitTestMode
    val scopes = HILCompletionContributor.SCOPES
  }

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is ILExpression) return
    val host = element.getHCLHost() ?: return
    val hostFile = host.containingFile
    if (hostFile !is HCLFile || !hostFile.isInterpolationsAllowed()) return

    if (element !is ILVariable) return
    val parent = element.parent
    if (parent is ILSelectExpression) {
      if (parent.from === element) {
        annotateLeftmostInSelection(element, holder)
      } else if (isScopeElementReference(element, parent)) {
        holder.createInfoAnnotation(element, if (DEBUG) "scope value reference" else null).textAttributes = HILSyntaxHighlighterFactory.TIL_PROPERTY_REFERENCE
      } else if (isResourceInstanceReference(element, parent)) {
        holder.createInfoAnnotation(element, if (DEBUG) "resource instance reference" else null).textAttributes = HILSyntaxHighlighterFactory.TIL_RESOURCE_INSTANCE_REFERENCE
      } else if (isResourcePropertyReference(element, parent)) {
        holder.createInfoAnnotation(element, if (DEBUG) "property reference" else null).textAttributes = HILSyntaxHighlighterFactory.TIL_PROPERTY_REFERENCE
      }
    }
  }

  private fun annotateLeftmostInSelection(element: ILVariable, holder: AnnotationHolder) {
    if (scopes.contains(element.name)) {
      holder.createInfoAnnotation(element, if (DEBUG) "global scope" else null).textAttributes = HILSyntaxHighlighterFactory.TIL_PREDEFINED_SCOPE
    } else {
      holder.createInfoAnnotation(element, if (DEBUG) "resource type reference" else null).textAttributes = HILSyntaxHighlighterFactory.TIL_RESOURCE_TYPE_REFERENCE
    }
  }

}

fun isScopeElementReference(element: ILVariable, parent: ILSelectExpression): Boolean {
  val left = getGoodLeftElement(parent, element, false) as? ILVariable ?: return false
  val lp = left.parent as? ILSelectExpression ?: return false
  return (left === lp.from) && HILVariableAnnotator.scopes.contains(left.name)
}

fun isResourceInstanceReference(element: ILVariable, parent: ILSelectExpression): Boolean {
  val left = getGoodLeftElement(parent, element, false) as? ILVariable ?: return false
  val lp = left.parent as? ILSelectExpression ?: return false
  return (left === lp.from) && !HILVariableAnnotator.scopes.contains(left.name)
}

fun isResourcePropertyReference(element: ILVariable, parent: ILSelectExpression): Boolean {
  // TODO: Improve. Somehow. See ILSelectFromSomethingReferenceProvider
  // Since this function called after another two, other variants are already used
  if (parent.from !is ILSelectExpression) return false
  getGoodLeftElement(parent, element) as? ILVariable ?: return false
  return true
}