/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.ModuleDetectionUtil
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns
import org.intellij.plugins.hcl.terraform.run.TerraformConfigurationType
import org.intellij.plugins.hcl.terraform.run.TerraformRunConfiguration

class TFMissingModuleInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val ft = holder.file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return MyEV(holder)
  }

  override fun getID(): String {
    return "MissingModule"
  }

  override fun getBatchSuppressActions(element: PsiElement?): Array<SuppressQuickFix> {
    return super.getBatchSuppressActions(PsiTreeUtil.getParentOfType(element, HCLBlock::class.java, false))
  }

  inner class MyEV(val holder: ProblemsHolder) : HCLElementVisitor() {
    override fun visitBlock(block: HCLBlock) {
      ProgressIndicatorProvider.checkCanceled()
      block.getNameElementUnquoted(0) ?: return
      block.`object` ?: return
      if (!TerraformPatterns.ModuleRootBlock.accepts(block)) return
      if (TerraformPatterns.ModuleWithEmptySource.accepts(block)) return
      doCheck(holder, block)
    }
  }

  private fun doCheck(holder: ProblemsHolder, block: HCLBlock) {
    val directory = block.containingFile.containingDirectory ?: return

    val pair = ModuleDetectionUtil.getAsModuleBlockOrError(block)
    if (pair.first != null) return
    val err = pair.second ?: "Unknown reason"

    ProgressIndicatorProvider.checkCanceled()

    holder.registerProblem(block, "Can't locate module locally: $err", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, RunTerraformGetFix(directory.name))
  }
}


class RunTerraformGetFix(name: String) : LocalQuickFixBase("Run `terraform get` in $name", "Run `terraform get`") {
  companion object {
    private val LOG = Logger.getInstance(RunTerraformGetFix::class.java)
  }

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val block = descriptor.psiElement as? HCLBlock ?: return
    val dir = block.containingFile?.containingDirectory ?: return

    val manager = RunManager.getInstance(project)
    val configurationSettings = manager.createRunConfiguration("terraform get in ${dir.name}", TerraformConfigurationType.getInstance().baseFactory)
    configurationSettings.isTemporary = true
    val configuration = configurationSettings.configuration as TerraformRunConfiguration
    val vf = dir.virtualFile
    if (vf.fileSystem !is LocalFileSystem) {
      LOG.warn("Cannot run on non-local FS: $vf")
      return
    }
    configuration.PROGRAM_PARAMETERS = "get"
    configuration.WORKING_DIRECTORY = vf.path

    try {
      ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configurationSettings).buildAndExecute()
    } catch (e: ExecutionException) {
      LOG.warn("Failed to run 'terraform get': ${e.message}", e)
      Messages.showMessageDialog(project, "Failed to run 'terraform get': ${e.message}",
          "Fetching Terraform Modules Failed", Messages.getErrorIcon())
    }
  }
}