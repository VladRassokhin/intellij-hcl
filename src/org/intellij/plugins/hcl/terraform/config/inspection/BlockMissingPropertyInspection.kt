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
package org.intellij.plugins.hcl.terraform.config.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.HCLObject
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator
import org.intellij.plugins.hcl.terraform.getNameElementUnquoted
import java.util.*

@Suppress("UNUSED_PARAMETER")
public class BlockMissingPropertyInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val ft = holder.file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  protected fun getTypeModel(): TypeModel {
    val provider = ServiceManager.getService(TypeModelProvider::class.java)
    return provider.get()
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      ProgressIndicatorProvider.checkCanceled()
      val bt = block.getNameElementUnquoted(0) ?: return
      block.`object` ?: return
      when (bt) {
        "atlas" -> doCheckAtlas(block, holder)
        "module" -> doCheckModule(block, holder)
        "output" -> doCheckOutput(block, holder)
        "provider" -> doCheckProvider(block, holder)
        "resource" -> doCheckResource(block, holder)
        "variable" -> doCheckVariable(block, holder)
        "provisioner" -> doCheckProvisioner(block, holder)
        "connection" -> doCheckConnection(block, holder)
      }
    }
  }

  private fun doCheckAtlas(block: HCLBlock, holder: ProblemsHolder) {
    val obj = block.`object` ?: return
    val properties = getTypeModel().Atlas.properties

    doCheck(block, holder, obj, properties)
  }

  private fun doCheckModule(block: HCLBlock, holder: ProblemsHolder) {
    val obj = block.`object` ?: return
    val properties = getTypeModel().Module.properties
    // TODO: Check module required properties basing on 'source'
    doCheck(block, holder, obj, properties)
  }

  private fun doCheckOutput(block: HCLBlock, holder: ProblemsHolder) {
    val obj = block.`object` ?: return
    val properties = getTypeModel().Output.properties

    doCheck(block, holder, obj, properties)
  }

  private fun doCheckProvider(block: HCLBlock, holder: ProblemsHolder) {
  }

  private fun doCheckResource(block: HCLBlock, holder: ProblemsHolder) {
    val type = block.getNameElementUnquoted(1) ?: return;
    val obj = block.`object` ?: return
    val rt = getTypeModel().getResourceType(type) ?: return // TODO: report unknown resource type (separate inspection)s

    doCheck(block, holder, obj, rt.properties)
  }

  private fun doCheck(block: HCLBlock, holder: ProblemsHolder, obj: HCLObject, properties: Array<out PropertyOrBlockType>) {
    ProgressIndicatorProvider.checkCanceled()

    val candidates = ArrayList<PropertyOrBlockType>(properties.filter { it.required })
    if (candidates.isEmpty()) return
    val all = ArrayList<String>()
    all.addAll(obj.propertyList.map { it.name })
    all.addAll(obj.blockList.map { it.name }) // TODO: Better block name selection

    ProgressIndicatorProvider.checkCanceled()

    val required = candidates.filterNot { it.name in all }

    if (required.isEmpty()) return

    ProgressIndicatorProvider.checkCanceled()

    holder.registerProblem(block, "Missing required properties: ${required.map { it.name }.join(", ")}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, AddResourcePropertiesFix(required))
  }

  private fun doCheckVariable(block: HCLBlock, holder: ProblemsHolder) {
  }

  private fun doCheckProvisioner(block: HCLBlock, holder: ProblemsHolder) {
  }

  private fun doCheckConnection(block: HCLBlock, holder: ProblemsHolder) {
  }

}

class AddResourcePropertiesFix(val add: Collection<PropertyOrBlockType>) : LocalQuickFixBase("Add properties: ${add.map { it.name }.join(", ")}", "Add missing properties") {
  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val element = descriptor.psiElement
    if (element !is HCLBlock) return
    val block = element;
    val obj = block.`object` ?: return
    object : WriteCommandAction<Any?>(project) {
      override fun run(result: Result<Any?>) {
        val generator = TerraformElementGenerator(project)
        val elements = add.map {
          if (it.property != null) generator.createProperty(it.name, "\"\"");
          else generator.createBlock(it.name)
        }
        @Suppress("UNUSED_VARIABLE")
        val added = elements.map { obj.addBefore(it, obj.lastChild) }
        // TODO: Investigate why reformat fails
        // CodeStyleManager.getInstance(project).reformat(block, true)
        // TODO: Navigate cursor to added.last() or added.first()
      }
    }.execute()
  }
}