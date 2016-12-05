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
package org.intellij.plugins.hil.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hil.HILElementTypes
import org.intellij.plugins.hil.HILElementTypes.IL_BINARY_EQUALITY_EXPRESSION
import org.intellij.plugins.hil.HILTypes.ILBinaryBooleanOnlyOperations
import org.intellij.plugins.hil.HILTypes.ILBinaryNumericOnlyOperations
import org.intellij.plugins.hil.psi.*

class HILOperationTypesMismatchInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val file = InjectedLanguageManager.getInstance(holder.project).getTopLevelFile(holder.file)
    val ft = file.fileType
    if (ft != TerraformFileType) {
      return super.buildVisitor(holder, isOnTheFly)
    }
    return MyEV(holder)
  }

  inner class MyEV(val holder: ProblemsHolder) : ILElementVisitor() {
    override fun visitILBinaryExpression(operation: ILBinaryExpression) {
      ProgressIndicatorProvider.checkCanceled()
      val host = InjectedLanguageManager.getInstance(operation.project).getInjectionHost(operation) ?: return
      if (host !is HCLElement) return

      val left = operation.lOperand
      val right = operation.rOperand ?: return
      val os = operation.operationSign

      // Return if we cannot determine operands type
      val leftType = getType(left) ?: return
      val rightType = getType(right) ?: return

      val elementType = operation.node.elementType
      if (elementType in ILBinaryNumericOnlyOperations) {
        if (leftType != Types.Number || rightType != Types.Number) {
          holder.registerProblem(operation, "Both arguments expected to be numbers. left is ${leftType.name}, right is ${rightType.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else if (elementType == IL_BINARY_EQUALITY_EXPRESSION) {
        return // could compare anything with implicit 'toString' conversion. TODO: Add warning?
      } else if (elementType in ILBinaryBooleanOnlyOperations) {
        if (leftType != Types.Boolean || rightType != Types.Boolean) {
          holder.registerProblem(operation, "Both arguments expected to be booleans. left is ${leftType.name}, right is ${rightType.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else {
        return
      }
    }

    override fun visitILUnaryExpression(operation: ILUnaryExpression) {
      ProgressIndicatorProvider.checkCanceled()
      val host = InjectedLanguageManager.getInstance(operation.project).getInjectionHost(operation) ?: return
      if (host !is HCLElement) return

      val operand = operation.operand ?: return
      val sign = operation.operationSign

      // Return if we cannot determine operands type
      val type = getType(operand) ?: return


      if (sign == HILElementTypes.OP_PLUS || sign == HILElementTypes.OP_MINUS) {
        if (type != Types.Number) {
          holder.registerProblem(operand, "Expected to be number, actual type is ${type.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else if (sign == HILElementTypes.OP_NOT) {
        if (type != Types.Boolean) {
          holder.registerProblem(operand, "Expected to be boolean, actual type is ${type.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else {
        return
      }
    }
  }

  private fun getType(e: ILExpression): Type? {
    return when (e) {
      is ILLiteralExpression -> e.type

    // TODO: Implement via resolving/model :
      is ILVariable -> null
      is ILExpressionHolder -> null
      is ILBinaryExpression -> null
      is ILSelectExpression -> null
      is ILMethodCallExpression -> null
      is ILConditionalExpression -> null
      is ILUnaryExpression -> null
      is ILParenthesizedExpression -> null
    //is ILParameterList -> null
      else -> null
    }
  }

}