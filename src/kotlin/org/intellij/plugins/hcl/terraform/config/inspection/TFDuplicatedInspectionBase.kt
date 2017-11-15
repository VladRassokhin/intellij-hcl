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
import com.intellij.ide.DataManager
import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandlerFactory
import com.intellij.refactoring.RefactoringFactory
import com.intellij.usageView.UsageInfo
import com.intellij.usages.*
import com.intellij.util.Consumer
import com.intellij.util.NullableFunction
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.getTerraformSearchScope
import org.jetbrains.annotations.NotNull


abstract class TFDuplicatedInspectionBase : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = holder.file
    if (file.fileType != TerraformFileType || file.name.endsWith("." + TerraformFileType.TFVARS_EXTENSION)) {
      return super.buildVisitor(holder, isOnTheFly)
    }

    return createVisitor(holder)
  }

  companion object {
    abstract class RenameQuickFix(name: String) : LocalQuickFixBase(name) {
      protected fun invokeRenameRefactoring(project: Project, element: PsiElement) {
        // TODO: Find way to remove only current element

        val factory = RefactoringFactory.getInstance(project)
        val renameRefactoring = factory.createRename(element, "newName")
        renameRefactoring.isSearchInComments = false
        renameRefactoring.isSearchInNonJavaFiles = false
        renameRefactoring.run()
        if (true) return

        val renameHandler = RefactoringActionHandlerFactory.getInstance().createRenameHandler()
        val dataManager = DataManager.getInstance()
        if (ApplicationManager.getApplication().isUnitTestMode) {
          @Suppress("DEPRECATION")
          renameHandler.invoke(project, arrayOf(element), dataManager.dataContext)
          return
        }
        dataManager.dataContextFromFocus.doWhenDone(Consumer { context: DataContext? ->
          context?.let {
            ApplicationManager.getApplication().invokeLater(Runnable {
              renameHandler.invoke(project, arrayOf(element), context)
            }, project.disposed)
          }
        })
      }
    }
  }

  abstract fun createVisitor(holder: ProblemsHolder): PsiElementVisitor

  protected fun createNavigateToDupeFix(file: VirtualFile, offsetInOtherFile: Int, single: Boolean): LocalQuickFix? {
    return object : LocalQuickFixBase("Navigate to ${if (!single) "first " else ""}duplicate") {
      override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        OpenFileDescriptor(project, file, offsetInOtherFile).navigate(true)
      }
    }
  }

  protected fun createShowOtherDupesFix(file: VirtualFile, offset: Int, duplicates: NullableFunction<PsiElement, List<PsiElement>?>): LocalQuickFix? {

    return object : LocalQuickFixBase("View duplicates like this") {
      override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        @Suppress("NAME_SHADOWING")
        val duplicates = duplicates.`fun`(descriptor.psiElement) ?: return

        val presentation = UsageViewPresentation()
        val title = buildTitle()
        presentation.usagesString = title
        presentation.tabName = title
        presentation.tabText = title
        val scope = descriptor.psiElement.getTerraformSearchScope()
        presentation.scopeText = scope.displayName

        UsageViewManager.getInstance(project).searchAndShowUsages(arrayOf<UsageTarget>(object : UsageTarget {
          override fun findUsages() {}

          override fun findUsagesInEditor(@NotNull editor: FileEditor) {}

          override fun highlightUsages(@NotNull file: PsiFile, @NotNull editor: Editor, clearHighlights: Boolean) {}

          override fun isValid(): Boolean {
            return true
          }

          override fun isReadOnly(): Boolean {
            return true
          }

          override fun getFiles(): Array<VirtualFile>? {
            return null
          }

          override fun update() {}

          @NotNull
          override fun getName(): String? {
            return buildTitle()
          }

          @NotNull
          override fun getPresentation(): ItemPresentation? {
            return PresentationData(name, "", null, null)
          }

          override fun navigate(requestFocus: Boolean) {
            OpenFileDescriptor(project, file, offset).navigate(requestFocus)
          }

          override fun canNavigate(): Boolean {
            return true
          }

          override fun canNavigateToSource(): Boolean {
            return canNavigate()
          }
        }), {
          UsageSearcher { processor ->
            val infos = ApplicationManager.getApplication().runReadAction<List<UsageInfo>> {
              duplicates.map { dup -> UsageInfo(dup.containingFile) }
            }
            for (info in infos) {
              processor.process(UsageInfo2UsageAdapter(info))
            }
          }
        }, false, false, presentation, null)
      }

      var myTitle: String? = null
      private fun buildTitle(): String {
        if (myTitle == null) myTitle = "Duplicate code like in " + file.name + ":" + offset
        return myTitle!!
      }
    }
  }
}

