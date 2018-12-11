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

import com.intellij.codeInspection.*
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.codeinsight.ResourcePropertyInsertHandler
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.ConfigOverrideFile
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.ModuleWithEmptySource
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator
import java.util.*

class HCLBlockMissingPropertyInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return buildVisitor(holder, isOnTheFly, false)
  }

  fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, recursive: Boolean): PsiElementVisitor {
    val ft = holder.file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder, recursive)
  }

  override fun getID(): String {
    return "MissingProperty"
  }

  override fun getBatchSuppressActions(element: PsiElement?): Array<SuppressQuickFix> {
    return super.getBatchSuppressActions(PsiTreeUtil.getParentOfType(element, HCLBlock::class.java, false))
  }

  inner class MyEV(val holder: ProblemsHolder, val recursive: Boolean) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      ProgressIndicatorProvider.checkCanceled()
      block.getNameElementUnquoted(0) ?: return
      val obj = block.`object` ?: return
      // TODO: Generify
      if (ModuleWithEmptySource.accepts(block)) return
      if (ConfigOverrideFile.accepts(block.containingFile)) return
      val properties = ModelHelper.getBlockProperties(block)
      doCheck(block, holder, properties)
      if (recursive) {
        visitElement(obj)
      }
    }

    override fun visitElement(element: HCLElement) {
      super.visitElement(element)
      if (recursive) {
        element.acceptChildren(this)
      }
    }
  }

  private fun doCheck(block: HCLBlock, holder: ProblemsHolder, properties: Array<out PropertyOrBlockType>) {
    if (properties.isEmpty()) return
    val obj = block.`object` ?: return
    ProgressIndicatorProvider.checkCanceled()

    val candidates = ArrayList<PropertyOrBlockType>(properties.filter { it.required && !(it.property?.has_default ?: false) })
    if (candidates.isEmpty()) return
    val all = ArrayList<String>()
    all.addAll(obj.propertyList.map { it.name })
    all.addAll(obj.blockList.map { it.name }) // TODO: Better block name selection

    ProgressIndicatorProvider.checkCanceled()

    val required = candidates.filterNot { it.name in all }

    if (required.isEmpty()) return

    ProgressIndicatorProvider.checkCanceled()

    holder.registerProblem(block, "Missing required properties: ${required.map { it.name }.joinToString(", ")}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, AddResourcePropertiesFix(required))
  }

}

class AddResourcePropertiesFix(val add: Collection<PropertyOrBlockType>) : LocalQuickFixBase("Add properties: ${add.map { it.name }.joinToString(", ")}", "Add missing properties") {
  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement as? HCLBlock ?: return
    val block = element
    val obj = block.`object` ?: return
    object : WriteCommandAction<Any?>(project) {
      override fun run(result: Result<Any?>) {
        val generator = TerraformElementGenerator(project)
        val elements = add.map {
          if (it.property != null) {
            val type = it.property.type
            // TODO: Use property 'default' value
            var value: String = ResourcePropertyInsertHandler.getPlaceholderValue(type)?.first ?:
                if (type == Types.Boolean) "false"
                else if (type == Types.Number) "0"
                else if (type == Types.Null) "null"
                else "\"\""

            value = ResourcePropertyInsertHandler.getProposedValueFromModelAndHint(it.property, element.getTerraformModule())?.first ?: value

            generator.createProperty(it.name, value)
          } else generator.createBlock(it.name)
        }
        for (it in elements) {
          obj.addBefore(it, obj.lastChild)
          obj.node.addLeaf(TokenType.WHITE_SPACE, "\n", obj.node.lastChildNode)
        }
        // TODO: Investigate why reformat fails
        // CodeStyleManager.getInstance(project).reformat(block, true)
        // TODO: Navigate cursor to added.last() or added.first()
      }
    }.execute()
  }
}
