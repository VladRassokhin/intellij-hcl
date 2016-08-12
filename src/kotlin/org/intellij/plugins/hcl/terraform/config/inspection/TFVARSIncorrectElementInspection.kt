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
import org.intellij.plugins.hcl.psi.impl.HCLStringLiteralMixin
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.ModelUtil
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule

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
      holder.registerProblem(block, "Only 'key=value' elements allowed", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }

    override fun visitProperty(property: HCLProperty) {
      val value = property.value ?: return
      if (property.parent is HCLFile) {
        if (value !is HCLNumberLiteral && value !is HCLObject && value !is HCLArray) {
          if (value !is HCLStringLiteral || value.quoteSymbol != '"') {
            holder.registerProblem(value, "Property value should be either number, double quoted string, list or object", *getQuoteFix(value))
          }
        }
        val vName = property.name.substringBefore('.')
        val variable = property.getTerraformModule().findVariable(vName)
        if (variable == null) {
          // TODO: Add 'Define variable' quick fix.
          holder.registerProblem(property.nameElement, "Undefined variable '$vName'", ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
        } else if (!property.name.contains('.')) {
          val expected = variable.second.`object`?.findProperty(TypeModel.Variable_Type.name)?.value?.name ?: "string"
          val actual = ModelUtil.getValueType(value)
          if ((expected == "string" && actual !in Types.SimpleValueTypes)
              || (expected == "list" && actual != Types.Array)
              || (expected == "map" && actual != Types.Object)) {
            val e = if (expected == "string") "simple value (string or number)" else "'$expected'"
            holder.registerProblem(value, "Incorrect variable value type, expected $e")
          }
        }
      }
    }
  }

  private fun getQuoteFix(element: HCLValue): Array<LocalQuickFix> {
    if (element is HCLStringLiteralMixin || element is HCLIdentifier || element is HCLNullLiteral || element is HCLBooleanLiteral) {
      return arrayOf(ConvertToString)
    }
    return emptyArray()
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
