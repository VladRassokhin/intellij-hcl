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
package org.intellij.plugins.hcl.terraform.config.refactoring

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.IntroduceTargetChooser
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.intellij.plugins.hcl.HCLBundle
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.navigation.HCLQualifiedNameProvider
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.model.PropertyType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator
import org.intellij.plugins.hil.refactoring.ILIntroduceVariableHandler
import org.intellij.plugins.hil.refactoring.ILRefactoringUtil
import java.util.*

class TerraformIntroduceVariableHandler : BaseIntroduceVariableHandler<HCLElement>() {
  companion object {
    fun getSelectedExpression(project: Project,
                              file: PsiFile,
                              element1: PsiElement,
                              element2: PsiElement): HCLElement? {
      var parent = PsiTreeUtil.findCommonParent(element1, element2)
      if (parent != null && parent !is HCLElement) {
        parent = PsiTreeUtil.getParentOfType(parent, HCLElement::class.java)
      }
      if (parent == null) {
        return null
      }
      if (parent !is HCLElement) {
        return null
      }
      if (element1 === PsiTreeUtil.getDeepestFirst(parent) && element2 === PsiTreeUtil.getDeepestLast(parent)) {
        return parent
      }
      return null
    }

  }

  override fun createOperation(editor: Editor, file: PsiFile, project: Project) = IntroduceOperation(project, editor, file, null)

  override fun performAction(operation: BaseIntroduceOperation<HCLElement>) {
    if (operation !is IntroduceOperation) return
    val file = operation.file
    if (!CommonRefactoringUtil.checkReadOnlyStatus(file)) {
      return
    }
    val editor = operation.editor
    if (editor.settings.isVariableInplaceRenameEnabled) {
      val templateState = TemplateManagerImpl.getTemplateState(operation.editor)
      if (templateState != null && !templateState.isFinished) {
        return
      }
    }

    var element1: PsiElement? = null
    var element2: PsiElement? = null
    val selectionModel = editor.selectionModel
    if (selectionModel.hasSelection()) {
      element1 = file.findElementAt(selectionModel.selectionStart)
      element2 = file.findElementAt(selectionModel.selectionEnd - 1)
      if (element1 is PsiWhiteSpace) {
        val startOffset = element1.textRange.endOffset
        element1 = file.findElementAt(startOffset)
      }
      if (element2 is PsiWhiteSpace) {
        val endOffset = element2.textRange.startOffset
        element2 = file.findElementAt(endOffset - 1)
      }
    } else {
      if (smartIntroduce(operation)) {
        return
      }
      val caretModel = editor.caretModel
      val document = editor.document
      val lineNumber = document.getLineNumber(caretModel.offset)
      if (lineNumber >= 0 && lineNumber < document.lineCount) {
        element1 = file.findElementAt(document.getLineStartOffset(lineNumber))
        element2 = file.findElementAt(document.getLineEndOffset(lineNumber) - 1)
      }
    }
    val project = operation.project
    if (element1 == null || element2 == null) {
      showCannotPerformError(project, editor)
      return
    }

    element1 = getSelectedExpression(project, file, element1, element2)
    if (element1 == null || !isValidIntroduceVariant(element1)) {
      showCannotPerformError(project, editor)
      return
    }

    if (!checkIntroduceContext(file, editor, element1)) {
      return
    }
    operation.element = element1
    performActionOnElement(operation)
  }

  private fun smartIntroduce(operation: IntroduceOperation): Boolean {
    val editor = operation.editor
    val file = operation.file
    val offset = editor.caretModel.offset
    var elementAtCaret = file.findElementAt(offset)
    if ((elementAtCaret is PsiWhiteSpace && offset == elementAtCaret.textOffset || elementAtCaret == null) && offset > 0) {
      elementAtCaret = file.findElementAt(offset - 1)
    }
    if (!checkIntroduceContext(file, editor, elementAtCaret)) return true
    val expressions = ArrayList<HCLElement>()
    while (elementAtCaret != null) {
      if (elementAtCaret is HCLBlock || elementAtCaret is HCLFile) {
        break
      }
      if (elementAtCaret is HCLElement && isValidIntroduceVariant(elementAtCaret)) {
        expressions.add(elementAtCaret)
      }
      elementAtCaret = elementAtCaret.parent
    }
    if (expressions.size == 1 || ApplicationManager.getApplication().isUnitTestMode) {
      operation.element = expressions[0]
      performActionOnElement(operation)
      return true
    } else if (expressions.size > 1) {
      IntroduceTargetChooser.showChooser(editor, expressions, object : Pass<HCLElement>() {
        override fun pass(expression: HCLElement) {
          operation.element = expression
          performActionOnElement(operation)
        }
      }, HCLElement::getText)
      return true
    }
    return false
  }


  protected fun checkIntroduceContext(file: PsiFile, editor: Editor, element: PsiElement?): Boolean {
    if (!isValidIntroduceContext(element)) {
      showCannotPerformError(file.project, editor)
      return false
    }
    return true
  }

  protected fun isValidIntroduceContext(element: PsiElement?): Boolean {
    // TODO: Investigate cases when refactoring should not be supported
    return element != null
  }

  private fun isValidIntroduceVariant(element: PsiElement): Boolean {
    // For now only property values could be replaces
    if (HCLParserDefinition.STRING_LITERALS.contains(element.node.elementType)) return isValidIntroduceVariant(element.parent)
    if (element !is HCLStringLiteral) return false
    val property = element.parent as? HCLProperty ?: return false
    return property.value === element
  }

  private fun performActionOnElement(operation: IntroduceOperation) {
    val element = operation.element
    val initializer = element as HCLElement?
    operation.initializer = initializer

    if (initializer != null) {
      operation.occurrences = getOccurrences(element, initializer)
      operation.suggestedNames = getSuggestedNames(initializer)
    }
    if (operation.occurrences.isEmpty()) {
      operation.isReplaceAll = false
    }

    performActionOnElementOccurrences(operation)
  }

  protected fun performActionOnElementOccurrences(operation: IntroduceOperation) {
    val editor = operation.editor
    if (editor.settings.isVariableInplaceRenameEnabled) {
      ensureName(operation)
      if (operation.isReplaceAll) {
        performInplaceIntroduce(operation)
      } else {
        OccurrencesChooser.simpleChooser<PsiElement>(editor).showChooser(operation.element, operation.occurrences, object : Pass<OccurrencesChooser.ReplaceChoice>() {
          override fun pass(replaceChoice: OccurrencesChooser.ReplaceChoice) {
            operation.isReplaceAll = replaceChoice == OccurrencesChooser.ReplaceChoice.ALL
            performInplaceIntroduce(operation)
          }
        })
      }
    } else {
      performIntroduceWithDialog(operation)
    }
  }


  protected fun performInplaceIntroduce(operation: IntroduceOperation) {
    val statement = performRefactoring(operation)
    if (statement is HCLBlock) {
      val target = statement.nameIdentifier!!
      val occurrences = operation.occurrences
      val occurrence = ILIntroduceVariableHandler.findOccurrenceUnderCaret(occurrences, operation.editor)
      operation.editor.caretModel.moveToOffset(target.textRange.startOffset)
      // TODO: Uncomment once have idea hw to change name of variable from it's usage
      val introducer: InplaceVariableIntroducer<PsiElement> = object : InplaceVariableIntroducer<PsiElement>(statement, operation.editor, operation.project, "Introduce Variable", operation.occurrences.toTypedArray(), null) {
        override fun checkLocalScope(): PsiElement? {
          return target.containingFile
        }
      }
      introducer.performInplaceRefactoring(LinkedHashSet(operation.suggestedNames))
    }
  }

  protected fun performIntroduceWithDialog(operation: IntroduceOperation) {
    val project = operation.project
    if (ApplicationManager.getApplication().isUnitTestMode) {
      ensureName(operation)
    }
    if (operation.name == null) {
      val dialog = VariableIntroduceDialog(project, HCLBundle.message("introduce.variable.title"), validator, operation)
      if (!dialog.showAndGet()) {
        return
      }
      operation.name = dialog.name
      operation.isReplaceAll = dialog.doReplaceAllOccurrences()
      // TODO: Support introducing in separate file
      //operation.setInitPlace(dialog.getInitPlace())
    }

    val declaration = performRefactoring(operation) ?: return
    val editor = operation.editor
    editor.caretModel.moveToOffset(declaration.textRange.endOffset)
    editor.selectionModel.removeSelection()
  }


  protected fun performRefactoring(operation: IntroduceOperation): PsiElement? {
    var declaration: PsiElement? = createDeclaration(operation)
    if (declaration == null) {
      showCannotPerformError(operation.project, operation.editor)
      return null
    }

    declaration = performReplace(declaration, operation)
    declaration = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(declaration)
    return declaration
  }

  private fun createDeclaration(operation: IntroduceOperation): PsiElement? {
    val expr = operation.initializer ?: return null
    val name = operation.name ?: return null
    val type: Type = expr.getType() ?: Types.String
    return TerraformElementGenerator(operation.project).createVariable(name, type, expr)
  }


  private fun performReplace(declaration: PsiElement,
                             operation: IntroduceOperation): PsiElement {
    val expression = operation.initializer!!
    val project = operation.project
    return object : WriteCommandAction<PsiElement>(project, expression.containingFile) {
      @Throws(Throwable::class)
      override fun run(result: Result<PsiElement>) {
        try {
          val afterData = RefactoringEventData()
          afterData.addElement(declaration)
          project.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
              .refactoringStarted(ILIntroduceVariableHandler.REFACTORING_ID, afterData)

          result.setResult(addDeclaration(operation, declaration))

          val newExpression = createExpression(project, operation.name!!)

          if (operation.isReplaceAll) {
            val newOccurrences = ArrayList<PsiElement>()
            for (occurrence in operation.occurrences) {
              val replaced = replaceExpression(occurrence, newExpression)
              newOccurrences.add(replaced)
            }
            operation.occurrences = newOccurrences
          } else {
            val replaced = replaceExpression(expression, newExpression)
            operation.occurrences = listOf(replaced)
          }
        } finally {
          val afterData = RefactoringEventData()
          afterData.addElement(declaration)
          project.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
              .refactoringDone(ILIntroduceVariableHandler.REFACTORING_ID, afterData)
        }
      }
    }.execute().resultObject
  }

  private fun createExpression(project: Project, name: String): PsiElement {
    return TerraformElementGenerator(project).createStringLiteral("\${var.$name}")
  }

  protected fun replaceExpression(expression: PsiElement, newExpression: PsiElement): PsiElement {
    return expression.replace(newExpression)
  }

  fun addDeclaration(operation: IntroduceOperation, declaration: PsiElement): PsiElement? {
    val anchor = if (operation.isReplaceAll) ILIntroduceVariableHandler.findAnchor(operation.occurrences) else ILIntroduceVariableHandler.findAnchor(operation.initializer!!)
    if (anchor == null) {
      CommonRefactoringUtil.showErrorHint(
          operation.project,
          operation.editor,
          RefactoringBundle.getCannotRefactorMessage(HCLBundle.message("refactoring.introduce.anchor.error")),
          HCLBundle.message("refactoring.introduce.error"), null
      )
      return null
    }
    return anchor.parent.addBefore(declaration, anchor)
  }


  protected fun getOccurrences(element: PsiElement?, expression: HCLElement): List<PsiElement> {
    // TODO: ???
    var context: PsiElement? = null //PsiTreeUtil.getParentOfType(element, ILExpressionHolder::class.java, true) ?: element
    if (context == null) {
      context = expression.containingFile
    }
    // TODO: Filter our occurrences in places where interpolations cannot be used, e.g. variable 'default' property
    return ILRefactoringUtil.getOccurrences(expression, context)
  }

  val validator = IntroduceValidator()

  fun getSuggestedNames(expression: HCLElement): Collection<String> {
    val candidates = generateSuggestedNames(expression)

    val res = candidates
        .filter { validator.checkPossibleName(it, expression) }
        .toMutableList()

    if (res.isEmpty()) {  // no available names found, generate disambiguated suggestions
      for (name in candidates) {
        var index = 1
        while (!validator.checkPossibleName(name + index, expression)) {
          index++
        }
        res.add(name + index)
      }
    }
    if (res.isEmpty()) {
      res += "a"
    }
    return res
  }

  protected fun ensureName(operation: IntroduceOperation) {
    if (operation.name == null) {
      val suggestedNames = operation.suggestedNames
      if (suggestedNames != null && suggestedNames.isNotEmpty()) {
        operation.name = suggestedNames.first()
      } else {
        operation.name = "x"
      }
    }
  }

  protected fun generateSuggestedNames(expression: HCLElement): Collection<String> {
    val candidates = LinkedHashSet<String>()
    val text = expression.text
    val parent = expression.parent
    if (parent is HCLProperty) {
      candidates.add(parent.name)
    }
    HCLQualifiedNameProvider.getQualifiedName(parent)?.let { candidates += it }
    if (text != null) {
      candidates.addAll(ILRefactoringUtil.generateNames(text))
    }
    val type: Type? = expression.getType()
    if (type != null) {
      candidates.addAll(ILRefactoringUtil.generateNamesByType(type.name))
    }
    return candidates
  }
}

private fun HCLElement.getType(): Type? {
  if (this is HCLProperty) {
    val pp = parent?.parent
    if (pp is HCLBlock) {
      val properties = ModelHelper.getBlockProperties(pp)
      return properties.filterIsInstance(PropertyType::class.java).firstOrNull { it.name == name }?.type
    }
  } else if (this is HCLLiteral) {
    val parent = parent as? HCLElement
    return parent?.getType()
  }
  return null
}
