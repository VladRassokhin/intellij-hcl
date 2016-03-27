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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.psi.impl.HCLIdentifierMixin
import org.intellij.plugins.hcl.psi.impl.HCLStringLiteralMixin
import org.intellij.plugins.hcl.terraform.config.TerraformFileType

class TFVARSIncorrectElementInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (file.fileType != TerraformFileType || !file.name.endsWith("." + TerraformFileType.TFVARS_EXTENSION)) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      ProgressIndicatorProvider.checkCanceled()
      if (block.parent !is HCLFile) return
      holder.registerProblem(block, "Only 'key=value' elements allowed in .tfvars files", ProblemHighlightType.GENERIC_ERROR)
    }

    override fun visitProperty(property: HCLProperty) {
      val nameElement = property.nameElement
      if (nameElement !is HCLIdentifier) {
        holder.registerProblem(nameElement, "Property key should be identifier in .tfvars files", ProblemHighlightType.GENERIC_ERROR, *getUnquoteFix(nameElement))
      }
      val value = property.value ?: return
      if (value !is HCLNumberLiteral) {
        if (value !is HCLStringLiteral || value.quoteSymbol != '"') {
          holder.registerProblem(value, "Property value should be either number or double quoted string in .tfvars files", ProblemHighlightType.GENERIC_ERROR, *getQuoteFix(value))
        }
      }
    }
  }

  private fun getUnquoteFix(element: HCLValue): Array<LocalQuickFix> {
    if (element !is HCLStringLiteral) return emptyArray()
    if (!HCLIdentifierMixin.IDENTIFIER_PATTERN.matcher(element.value).matches()) return emptyArray()
    return arrayOf(UnquoteStringLiteral)
  }

  private fun getQuoteFix(element: HCLValue): Array<LocalQuickFix> {
    if (element is HCLStringLiteralMixin || element is HCLIdentifier || element is HCLNullLiteral || element is HCLBooleanLiteral) {
      return arrayOf(ConvertToString)
    }
    return emptyArray()
  }

  object UnquoteStringLiteral : LocalQuickFixBase("Unquote string literal") {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement
      if (element !is HCLStringLiteral) return
      element.replace(HCLElementGenerator(project).createIdentifier(element.value))
    }
  }

  object ConvertToString : LocalQuickFixBase("Convert to double quoted string") {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement
      val text: String
      if (element is HCLIdentifier || element is HCLNullLiteral || element is HCLBooleanLiteral) {
        text = element.text
      } else if (element is HCLStringLiteral) {
        text = element.value
      } else return
      element.replace(HCLElementGenerator(project).createStringLiteral(text))
    }
  }
}
