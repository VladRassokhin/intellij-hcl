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
package org.intellij.plugins.hil.inspection

import com.intellij.BundleBase
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInspection.*
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceOwner
import com.intellij.xml.util.AnchorReference
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hil.codeinsight.isResourceInstanceReference
import org.intellij.plugins.hil.codeinsight.isResourcePropertyReference
import org.intellij.plugins.hil.codeinsight.isScopeElementReference
import org.intellij.plugins.hil.psi.ILElementVisitor
import org.intellij.plugins.hil.psi.ILSelectExpression
import org.intellij.plugins.hil.psi.ILVariable

class UnresolvedReferencedInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = InjectedLanguageManager.getInstance(holder.project).getTopLevelFile(holder.file)
    val ft = file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  companion object {
    private val LOG = Logger.getInstance(UnresolvedReferencedInspection::class.java)

  }

  inner class MyEV(val holder: ProblemsHolder) : ILElementVisitor() {
    override fun visitILVariable(element: ILVariable) {
      ProgressIndicatorProvider.checkCanceled()
      val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return
      if (host !is HCLElement) return

      if (element !is ILVariable) return
      val parent = element.parent
      if (parent !is ILSelectExpression) return
      if (parent.from === element) return

      if (isScopeElementReference(element, parent)) {
        // TODO: Check scope parameter reference
        checkReferences(element)
      } else if (isResourceInstanceReference(element, parent)) {
        // TODO: Check and report "no such resource of type" error
        checkReferences(element)
      } else if (isResourcePropertyReference(element, parent)) {
        // TODO: Check and report "no such resource property" error (only if there such resource)
        checkReferences(element)
      }
    }

    private fun checkReferences(value: PsiElement?) {
      if (value == null) return

      doCheckRefs(value, value.references, 0)
    }

    private fun doCheckRefs(value: PsiElement, references: Array<PsiReference>, start: Int) {
      for (i in start..references.size - 1) {
        val reference = references[i]
        ProgressManager.checkCanceled()
        if (isUrlReference(reference)) continue
        if (!hasBadResolve(reference, false)) {
          continue
        }
        val description = getErrorDescription(reference)

        //        val startOffset = reference.element.textRange.startOffset
        val referenceRange = reference.rangeInElement

        // logging for IDEADEV-29655
        if (referenceRange.startOffset > referenceRange.endOffset) {
          LOG.error("Reference range start offset > end offset:  " + reference +
              ", start offset: " + referenceRange.startOffset + ", end offset: " + referenceRange.endOffset)
        }

        var fixes: Array<out LocalQuickFix> = emptyArray()
        if (reference is LocalQuickFixProvider) {
          reference.quickFixes?.let { fixes = it }
        }
        holder.registerProblem(value, description, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, referenceRange/*.shiftRight(startOffset)*/, *fixes)
      }

    }
  }


  fun isUrlReference(reference: PsiReference): Boolean {
    return reference is FileReferenceOwner || reference is AnchorReference
  }

  fun getErrorDescription(reference: PsiReference): String {
    val message: String
    if (reference is EmptyResolveMessageProvider) {
      message = reference.unresolvedMessagePattern
    } else {
      message = "Unresolved reference {0}"
    }

    val description: String
    try {
      description = BundleBase.format(message, reference.canonicalText) // avoid double formatting
    } catch (ex: IllegalArgumentException) {
      // unresolvedMessage provided by third-party reference contains wrong format string (e.g. {}), tolerate it
      description = message
    }

    return description
  }

  fun hasBadResolve(reference: PsiReference, checkSoft: Boolean): Boolean {
    if (!checkSoft && reference.isSoft) return false
    if (reference is PsiFakeAwarePolyVariantReference) {
      return reference.multiResolve(false, true).size == 0
    }
    if (reference is PsiPolyVariantReference) {
      return reference.multiResolve(false).size == 0
    }
    return reference.resolve() == null
  }
}