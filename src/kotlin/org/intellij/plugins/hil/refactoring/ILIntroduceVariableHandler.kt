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
package org.intellij.plugins.hil.refactoring

import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.featureStatistics.ProductivityFeatureNames
import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Pass
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.*
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.introduceField.ElementToWorkOn
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.ui.ConflictsDialog
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.intellij.plugins.hcl.HCLBundle
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator
import org.intellij.plugins.hil.psi.ILElementGenerator
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.ILLiteralExpression
import org.intellij.plugins.hil.psi.ILParenthesizedExpression
import org.intellij.plugins.hil.psi.impl.HILPsiImplUtils
import org.intellij.plugins.hil.psi.impl.getHCLHost
import org.jetbrains.annotations.NonNls
import java.util.*

class ILIntroduceVariableHandler : RefactoringActionHandler {
  companion object {
    private val LOG = Logger.getInstance(ILIntroduceVariableHandler::class.java)
    val REFACTORING_NAME = HCLBundle.message("introduce.variable.title")!!
    @NonNls private val REFACTORING_ID = "hil.refactoring.extractVariable"
    fun getSuggestedName(expression: ILExpression, type: Type?): SuggestedNameInfo {
      // TODO: Improve
      val name = if (expression is ILLiteralExpression) expression.unquotedText else expression.text
      val candidates = arrayListOf(name)
      val suffix = when (type) {
        Types.Array -> "_list"
        Types.Object -> "_map"
        else -> ""
      }
      if (suffix.isNotEmpty()) {
        candidates += "$name$suffix"
      }
      return object : SuggestedNameInfo(candidates.toTypedArray()) {
      }
    }

    fun getSettings(expr: ILExpression, replaceChoice: OccurrencesChooser.ReplaceChoice?): IntroduceVariableSettings {
      val replaceAll = replaceChoice == OccurrencesChooser.ReplaceChoice.ALL || replaceChoice == OccurrencesChooser.ReplaceChoice.NO_WRITE
      val type: Type = expr.getType() ?: Types.String
      val name = getSuggestedName(expr, type).names.firstOrNull() ?: ""
      return object : IntroduceVariableSettings {
        override val name: String
          get() = name
        override val isReplaceAllOccurrences: Boolean
          get() = replaceAll
        override val type: Type
          get() = type
        override val isOK: Boolean
          get() = true
      }
    }

    fun reportConflicts(conflicts: MultiMap<PsiElement, String>, project: Project): Boolean {
      val dialog = ConflictsDialog(project, conflicts)
      dialog.show()
      val ok = dialog.isOK
      if (!ok && dialog.isShowConflicts) {
        if (dialog is DialogWrapper) (dialog as DialogWrapper).close(DialogWrapper.CANCEL_EXIT_CODE)
      }
      return ok
    }

    fun showErrorMessage(project: Project, editor: Editor?, message: String) {
      CommonRefactoringUtil.showErrorHint(project, editor, message, "Introduce variable", null)
    }

  }

  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
    throw UnsupportedOperationException()
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
    if (editor == null || file == null || dataContext == null) return
    val selectionModel = editor.selectionModel
    if (!selectionModel.hasSelection()) {
      // TODO: Expand selection like in IntroduceVariableBase.invoke()
      LOG.warn("Selection model has nothing selected")
      return
    }
    LOG.info("Selection model: " + selectionModel)
    selectionModel.selectionStart;selectionModel.selectionEnd
//    val offset = editor.caretModel.offset
//    val element = file.findElementAt(offset)
//    val expression = PsiTreeUtil.getParentOfType(element, ILExpression::class.java) ?: return
    if (invoke(project, editor, file, selectionModel.selectionStart, selectionModel.selectionEnd) && LookupManager.getActiveLookup(editor) == null) {
      selectionModel.removeSelection()
    }
  }

  fun invoke(project: Project, editor: Editor, file: PsiFile, startOffset: Int, endOffset: Int): Boolean {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("hcl." + ProductivityFeatureNames.REFACTORING_INTRODUCE_VARIABLE)
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    return invokeImpl(project, findExpressionInRange(project, file, startOffset, endOffset), editor)
  }

  private fun findExpressionInRange(project: Project, file: PsiFile, startOffset: Int, endOffset: Int): ILExpression? {
    var tempExpr: ILExpression? = ILCodeInsightUtil.findExpressionInRange(file, startOffset, endOffset)
    if (tempExpr == null) {
      tempExpr = getSelectedExpression(project, file, startOffset, endOffset)
    }
    return tempExpr
  }


  fun getSelectedExpression(project: Project, file: PsiFile, startOffset: Int, endOffset: Int): ILExpression? {
    @Suppress("NAME_SHADOWING")
    var startOffset = startOffset
    @Suppress("NAME_SHADOWING")
    var endOffset = endOffset
    val injectedLanguageManager = InjectedLanguageManager.getInstance(project)
    var elementAtStart = file.findElementAt(startOffset)
    if (elementAtStart == null || elementAtStart is PsiWhiteSpace) {
      elementAtStart = PsiTreeUtil.skipSiblingsForward(elementAtStart, PsiWhiteSpace::class.java)
      if (elementAtStart == null) {
        if (injectedLanguageManager.isInjectedFragment(file)) {
          return getSelectionFromInjectedHost(project, file, injectedLanguageManager, startOffset, endOffset)
        } else {
          return null
        }
      }
      startOffset = elementAtStart.textOffset
    }
    var elementAtEnd = file.findElementAt(endOffset - 1)
    if (elementAtEnd == null || elementAtEnd is PsiWhiteSpace || elementAtEnd is PsiComment) {
      elementAtEnd = PsiTreeUtil.skipSiblingsBackward(elementAtEnd, PsiWhiteSpace::class.java, PsiComment::class.java)
      if (elementAtEnd == null) return null
      endOffset = elementAtEnd.textRange.endOffset
    }

    if (endOffset <= startOffset) return null

    var elementAt = PsiTreeUtil.findCommonParent(elementAtStart, elementAtEnd)
    if (PsiTreeUtil.getParentOfType(elementAt, PsiExpression::class.java, false) == null) {
      if (injectedLanguageManager.isInjectedFragment(file)) {
        return getSelectionFromInjectedHost(project, file, injectedLanguageManager, startOffset, endOffset)
      }
      elementAt = null
    }
    if (elementAt is ILExpression) return elementAt
    return null
  }


  private fun getSelectionFromInjectedHost(project: Project, file: PsiFile,
                                           injectedLanguageManager: InjectedLanguageManager,
                                           startOffset: Int, endOffset: Int): ILExpression? {
    val host = injectedLanguageManager.getInjectionHost(file) ?: return null
    return getSelectedExpression(project, host.containingFile,
        injectedLanguageManager.injectedToHost(file, startOffset), injectedLanguageManager.injectedToHost(file, endOffset))
  }


  // TODO: Support replacing several occurrences
  private fun invokeImpl(project: Project, expr: ILExpression?, editor: Editor?): Boolean {
    if (LOG.isDebugEnabled) {
      LOG.debug("expression:" + expr)
    }

    if (expr == null || !expr.isPhysical) {
//      if (ReassignVariableUtil.reassign(editor!!)) return false
      if (expr == null) {
        showErrorMessage(project, editor, RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("selected.block.should.represent.an.expression")))
        return false
      }
    }


    val originalType: Type? = HILPsiImplUtils.getType(expr)
    if (originalType == null) {
      showErrorMessage(project, editor, RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("unknown.expression.type")))
      return false
    }

    if (Types.String != originalType) {
      showErrorMessage(project, editor, RefactoringBundle.getCannotRefactorMessage("Selected expression should have 'String' type"))
      return false
    }


    val physicalElement = expr.getUserData(ElementToWorkOn.PARENT)

    val file = physicalElement?.containingFile ?: expr.containingFile

    LOG.assertTrue(file != null, "expr.getContainingFile() == null")

    val anchor = PsiTreeUtil.getTopmostParentOfType(expr.getHCLHost(), HCLBlock::class.java)
    if (anchor == null) {
      LOG.warn("Cannot find HCLBlock host for expr $expr")
      return false
    }

    val nameSuggestionContext = if (editor == null) null else file!!.findElementAt(editor.caretModel.offset)
    val supportProvider = LanguageRefactoringSupport.INSTANCE.forLanguage(expr.language)
    val isInplaceAvailableOnDataContext = supportProvider != null &&
        editor!!.settings.isVariableInplaceRenameEnabled &&
        supportProvider.isInplaceIntroduceAvailable(expr, nameSuggestionContext) &&
        !ApplicationManager.getApplication().isUnitTestMode


    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file!!)) return false

    val occurrencesMap = ContainerUtil.newLinkedHashMap<OccurrencesChooser.ReplaceChoice, List<ILExpression>>()
    occurrencesMap.put(OccurrencesChooser.ReplaceChoice.NO, listOf(expr))

    val wasSucceed = booleanArrayOf(true)
    val callback = object : Pass<OccurrencesChooser.ReplaceChoice>() {
      override fun pass(choice: OccurrencesChooser.ReplaceChoice?) {
        val settings = getSettings(expr, choice)

//        val validator = InputValidator(project, expr)
//        if (!validator.isOK(settings)) return

        if (choice != null) {
          val inplaceIntroducer = VariableInplaceIntroducer(project, settings, expr, editor!!, expr, arrayOf(expr), REFACTORING_NAME)
          if (inplaceIntroducer.startInplaceIntroduceTemplate()) {
            return
          }
        }
        CommandProcessor.getInstance().executeCommand(project,
            Runnable {
              val topLevelEditor: Editor
              if (!InjectedLanguageManager.getInstance(project).isInjectedFragment(expr.containingFile)) {
                topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(editor!!)
              } else {
                topLevelEditor = editor!!
              }

              var variable: HCLBlock? = null
              try {
                if (!settings.isOK) {
                  wasSucceed[0] = false
                  return@Runnable
                }

                val beforeData = RefactoringEventData()
                beforeData.addElement(expr)
                project.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringStarted(REFACTORING_ID, beforeData)

                variable = ApplicationManager.getApplication().runWriteAction(introduce(project, expr, topLevelEditor, emptyArray(), anchor, settings))
              } finally {
                val afterData = RefactoringEventData()
                afterData.addElement(variable)
                project.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringDone(REFACTORING_ID, afterData)
              }
            }, REFACTORING_NAME, null)
      }
    }

    if (!isInplaceAvailableOnDataContext) {
      callback.pass(null)
    } else {
      val choice = getOccurrencesChoice()
      if (choice != null) {
        callback.pass(choice)
      } else {
        OccurrencesChooser.simpleChooser<ILExpression>(editor).showChooser(callback, occurrencesMap)
      }
    }
    return wasSucceed[0]
  }

  fun getOccurrencesChoice(): OccurrencesChooser.ReplaceChoice? = null

  fun introduce(project: Project,
                expr: ILExpression,
                editor: Editor?,
                occurrences: Array<ILExpression>,
                anchor: HCLBlock,
                settings: IntroduceVariableSettings): Computable<HCLBlock?> {
    val container = anchor.parent

    return Computable {
      try {
        val name = settings.name

        var declaration = TerraformElementGenerator(project).createVariable(name, settings.type, expr)
        declaration = container.addBefore(declaration, anchor) as HCLBlock
        // TODO: Add whitespace after variable block
        LOG.assertTrue(expr.isValid)
        LOG.assertTrue(declaration.isValid)

        val ref = ILElementGenerator(project).createVarReference(name)
        if (settings.isReplaceAllOccurrences) {
          val array = occurrences.mapTo(ArrayList<PsiElement>()) { outermostParenthesizedILExpression(it).replace(ref) }
          highlightReplacedOccurrences(project, editor, PsiUtilCore.toPsiElementArray(array))
        } else {
          expr.replace(ref)
        }
        return@Computable declaration
      } catch (e: IncorrectOperationException) {
        LOG.error(e)
      }
      null
    }
  }

  private fun highlightReplacedOccurrences(project: Project, editor: Editor?, replacedOccurrences: Array<PsiElement>) {
    if (editor == null) return
    if (ApplicationManager.getApplication().isUnitTestMode) return
    val highlightManager = HighlightManager.getInstance(project)
    val colorsManager = EditorColorsManager.getInstance()
    val attributes = colorsManager.globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
    highlightManager.addOccurrenceHighlights(editor, replacedOccurrences, attributes, true, null)
    WindowManager.getInstance().getStatusBar(project)!!.info = RefactoringBundle.message("press.escape.to.remove.the.highlighting")
  }


  private fun outermostParenthesizedILExpression(expr: ILExpression): ILExpression {
    var e: ILExpression = expr
    while (e.parent is ILParenthesizedExpression) {
      e = e.parent as ILParenthesizedExpression
    }
    return e
  }

}

private fun ILExpression.getType(): Type? {
  return HILPsiImplUtils.getType(this)
}
