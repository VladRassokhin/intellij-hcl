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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.BlockType
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator
import getNameElementUnquoted
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionProvider
import java.util.*

public class BlockMissingPropertyInspection : LocalInspectionTool() {
  companion object {
    private val LOG = Logger.getInstance(TerraformConfigCompletionProvider::class.java)
  }

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

      // Inner for 'resource'
        "lifecycle" -> doCheckLifecycle(block, holder)
        "provisioner" -> doCheckProvisioner(block, holder)
      // Can be inner for both 'resource' and 'provisioner'
        "connection" -> doCheckConnection(block, holder)
      }
    }
  }

  private fun doCheckAtlas(block: HCLBlock, holder: ProblemsHolder) {
    doCheck(block, holder, TypeModel.Atlas)
  }

  private fun doCheckModule(block: HCLBlock, holder: ProblemsHolder) {
    // TODO: Check module required properties basing on 'source'
    doCheck(block, holder, TypeModel.Module)
  }

  private fun doCheckOutput(block: HCLBlock, holder: ProblemsHolder) {
    doCheck(block, holder, TypeModel.Output)
  }

  private fun doCheckProvider(block: HCLBlock, holder: ProblemsHolder) {
    val type = block.getNameElementUnquoted(1) ?: return;
    val rt = getTypeModel().getProviderType(type) ?: return
    // TODO: report unknown provider type (separate inspection)s

    doCheck(block, holder, rt)
  }

  private fun doCheckResource(block: HCLBlock, holder: ProblemsHolder) {
    val type = block.getNameElementUnquoted(1) ?: return;
    val rt = getTypeModel().getResourceType(type) ?: return
    // TODO: report unknown resource type (separate inspection)s

    doCheck(block, holder, rt)
  }

  private fun doCheck(block: HCLBlock, holder: ProblemsHolder, type: BlockType) {
    doCheck(block, holder, type.properties)
  }

  private fun doCheck(block: HCLBlock, holder: ProblemsHolder, properties: Array<out PropertyOrBlockType>) {
    val obj = block.`object` ?: return
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
    doCheck(block, holder, TypeModel.Variable)
  }

  private fun doCheckLifecycle(block: HCLBlock, holder: ProblemsHolder) {
    doCheck(block, holder, TypeModel.ResourceLifecycle)
  }

  private fun doCheckProvisioner(block: HCLBlock, holder: ProblemsHolder) {
    val type = block.getNameElementUnquoted(1) ?: return;
    val rt = getTypeModel().getProvisionerType(type) ?: return
    // TODO: report unknown provisioner type (separate inspection)s

    doCheck(block, holder, rt)
  }

  private fun doCheckConnection(block: HCLBlock, holder: ProblemsHolder) {
    val type = block.`object`?.findProperty("type")?.value
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.Connection.properties)
    if (type is HCLStringLiteral) {
      val v = type.value.toLowerCase().trim()
      when (v) {
        "ssh" -> properties.addAll(TypeModel.ConnectionPropertiesSSH)
        "winrm" -> properties.addAll(TypeModel.ConnectionPropertiesWinRM)
      // TODO: Support interpolation resolving
        else -> LOG.warn("Unsupported 'connection' block type '${type.value}'")
      }
    }
    if (type == null) {
      // ssh by default
      properties.addAll(TypeModel.ConnectionPropertiesSSH)
    }
    doCheck(block, holder, properties.toTypedArray())
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