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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.ModuleRootBlock
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.ResourceRootBlock
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.TerraformRootBlock
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.VariableRootBlock
import org.intellij.plugins.hil.ILLanguageInjector
import java.util.*

class TFNoInterpolationsAllowedInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (file.fileType != TerraformFileType || file.name.endsWith("." + TerraformFileType.TFVARS_EXTENSION)) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  companion object {
    val StringLiteralAnywhereInVariable: PsiElementPattern.Capture<HCLStringLiteral> =
        psiElement(HCLStringLiteral::class.java)
            .inside(true, VariableRootBlock)
    val HeredocContentAnywhereInVariable: PsiElementPattern.Capture<HCLHeredocContent> =
        psiElement(HCLHeredocContent::class.java)
            .inside(true, VariableRootBlock)

    val DependsOnPropertyOfResource: PsiElementPattern.Capture<HCLProperty> =
        psiElement(HCLProperty::class.java)
            .withSuperParent(1, HCLObject::class.java)
            .withSuperParent(2, ResourceRootBlock)
            .with(object : PatternCondition<HCLProperty?>("HCLProperty(depends_on)") {
              override fun accepts(t: HCLProperty, context: ProcessingContext?): Boolean {
                return t.name == "depends_on"
              }
            })
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      if (ModuleRootBlock.accepts(block)) {
        checkModule(block)
      } else if (TerraformRootBlock.accepts(block)) {
        checkTerraform(block)
      }
    }

    override fun visitStringLiteral(o: HCLStringLiteral) {
      if (StringLiteralAnywhereInVariable.accepts(o)) {
        checkForVariableInterpolations(o)
      }
    }

    override fun visitHeredocContent(o: HCLHeredocContent) {
      if (HeredocContentAnywhereInVariable.accepts(o)) {
        checkForVariableInterpolations(o)
      }
    }

    override fun visitProperty(o: HCLProperty) {
      if (DependsOnPropertyOfResource.accepts(o)) {
        checkDependsOnOfResource(o)
      }
    }

    private fun checkModule(block: HCLBlock) {
      // Ensure there's no interpolation in module 'source' string
      val source = block.`object`?.findProperty("source")?.value
      if (source != null) {
        if (source is HCLStringLiteral) {
          reportRanges(source, "module source")
        } else {
          holder.registerProblem(source, "Module source should be a double quoted string")
        }
      }
    }

    private fun checkTerraform(block: HCLBlock) {
      // Ensure there's no interpolation in all string properties
      (block.`object`?.propertyList ?: return)
          .map { it.value }
          .filterIsInstance<HCLStringLiteral>()
          .forEach { reportRanges(it, "properties inside 'terraform' block") }
    }

    private fun checkForVariableInterpolations(o: HCLStringLiteral) {
      reportRanges(o, "variables")
    }

    private fun checkForVariableInterpolations(o: HCLHeredocContent) {
      val ranges = ArrayList<TextRange>()
      ILLanguageInjector.getHCLHeredocContentInjections(o, getInjectedLanguagePlacesCollector(ranges))
      for (range in ranges) {
        holder.registerProblem(o, "Interpolations are not allowed in variables", ProblemHighlightType.ERROR, range)
      }
    }

    private fun checkDependsOnOfResource(o: HCLProperty) {
      val value = o.value as? HCLArray ?: return
      val list = value.expressionList
      for (e in list) {
        if (e is HCLStringLiteral) {
          reportRanges(e, "depends_on")
        }
      }
    }

    private fun reportRanges(e: HCLStringLiteral, where: String) {
      val ranges = ArrayList<TextRange>()
      ILLanguageInjector.getStringLiteralInjections(e, getInjectedLanguagePlacesCollector(ranges))
      for (range in ranges) {
        holder.registerProblem(e, "Interpolations are not allowed in $where", ProblemHighlightType.ERROR, range)
      }
    }

    private fun getInjectedLanguagePlacesCollector(ranges: ArrayList<TextRange>) =
        InjectedLanguagePlaces { _, rangeInsideHost, _, _ -> ranges.add(rangeInsideHost) }
  }
}

