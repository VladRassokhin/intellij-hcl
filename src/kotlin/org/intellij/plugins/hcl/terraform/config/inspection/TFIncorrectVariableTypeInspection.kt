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
import com.intellij.openapi.project.Project
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.ModelUtil
import org.intellij.plugins.hcl.terraform.config.model.SimpleValueHint
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hcl.terraform.config.psi.TerraformReferenceContributor

class TFIncorrectVariableTypeInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (file.fileType != TerraformFileType || file.name.endsWith("." + TerraformFileType.TFVARS_EXTENSION)) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  companion object {
    val VariableRootBlockSelector: PsiElementPattern.Capture<HCLBlock> =
        psiElement(HCLBlock::class.java)
            .withParent(TerraformReferenceContributor.TerraformConfigFile)
            .with(object : PatternCondition<HCLBlock?>("HCLBlock(variable)") {
              override fun accepts(t: HCLBlock, context: ProcessingContext?): Boolean {
                return t.getNameElementUnquoted(0) == "variable" && t.nameElements.size <= 2
              }
            })
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      if (!VariableRootBlockSelector.accepts(block)) return

      val obj = block.`object` ?: return

      val typeProperty = obj.findProperty(TypeModel.Variable_Type.name)
      val defaultProperty = obj.findProperty(TypeModel.Variable_Default.name)

      val expected = typeProperty?.value?.name ?: "string"

      val typeVariants = (TypeModel.Variable_Type.hint as SimpleValueHint).hint
      if (expected !in typeVariants) {
        typeProperty?.value?.let {
          holder.registerProblem(it, "Incorrect variable type, expected " +
              (if (typeVariants.size > 1) (typeVariants.dropLast(1).joinToString() + " or ") else "") + typeVariants.last(),
              ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
        }
      }

      val value = defaultProperty?.value ?: return
      val actual = ModelUtil.getValueType(value) ?: return

      if ((expected == "string" && actual !in Types.SimpleValueTypes)
          || (expected == "list" && actual != Types.Array)
          || (expected == "map" && actual != Types.Object)) {
        val to: String = when (actual) {
          in Types.SimpleValueTypes -> "string"
          Types.Array -> "list"
          Types.Object -> "map"
          else -> throw IllegalStateException()
        }
        holder.registerProblem(value, "Variable value type doesn't match default value type '$expected'", ChangeVariableType(to))
      }
    }
  }

  private class ChangeVariableType(val toType: String) : LocalQuickFixBase("Change variable type to $toType") {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val element = descriptor.psiElement
      if (element !is HCLValue) return
      val property = element.parent
      if (property !is HCLProperty) return
      val obj = property.parent
      if (obj !is HCLObject) return
      val typeProperty = obj.findProperty(TypeModel.Variable_Type.name)

      if (typeProperty == null) {
        obj.addAfter(HCLElementGenerator(project).createProperty("type", "\"$toType\""), obj.firstChild)
      } else {
        // Replace property value, though it may create errors in .tfvars
        typeProperty.value!!.replace(HCLElementGenerator(project).createStringLiteral(toType))
      }
    }
  }
}
