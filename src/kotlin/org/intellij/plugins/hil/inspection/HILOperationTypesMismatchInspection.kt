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
package org.intellij.plugins.hil.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElementVisitor
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hil.GoUtil
import org.intellij.plugins.hil.HILElementTypes
import org.intellij.plugins.hil.HILElementTypes.IL_BINARY_EQUALITY_EXPRESSION
import org.intellij.plugins.hil.HILTypes.ILBinaryBooleanOnlyOperations
import org.intellij.plugins.hil.HILTypes.ILBinaryNumericOnlyOperations
import org.intellij.plugins.hil.psi.*
import org.intellij.plugins.hil.psi.impl.getHCLHost

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
      operation.getHCLHost() ?: return

      val left = operation.lOperand
      val right = operation.rOperand ?: return

      val leftType = getType(left)
      val rightType = getType(right)

      val elementType = operation.node.elementType
      if (elementType in ILBinaryNumericOnlyOperations) {
        if (leftType != null && leftType != Types.Number && leftType != Types.Any) {
          holder.registerProblem(left, "Expected to be number, actual type is ${leftType.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        if (rightType != null && rightType != Types.Number && rightType != Types.Any) {
          holder.registerProblem(right, "Expected to be number, actual type is ${rightType.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else if (elementType == IL_BINARY_EQUALITY_EXPRESSION) {
        return // could compare anything with implicit 'toString' conversion. TODO: Add warning?
      } else if (elementType in ILBinaryBooleanOnlyOperations) {
        if (leftType != null && leftType != Types.Boolean && leftType != Types.Any) {
          holder.registerProblem(left, "Expected to be boolean, actual type is ${leftType.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        if (rightType != null && rightType != Types.Boolean && rightType != Types.Any) {
          holder.registerProblem(right, "Expected to be boolean, actual type is ${rightType.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else {
        return
      }
    }

    override fun visitILUnaryExpression(operation: ILUnaryExpression) {
      ProgressIndicatorProvider.checkCanceled()
      operation.getHCLHost() ?: return

      val operand = operation.operand ?: return
      val sign = operation.operationSign

      // Return if we cannot determine operands type
      val type = getType(operand) ?: return


      if (sign == HILElementTypes.OP_PLUS || sign == HILElementTypes.OP_MINUS) {
        if (type != Types.Number && type != Types.Any) {
          holder.registerProblem(operand, "Expected to be number, actual type is ${type.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else if (sign == HILElementTypes.OP_NOT) {
        if (type != Types.Boolean && type != Types.Any) {
          holder.registerProblem(operand, "Expected to be boolean, actual type is ${type.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
        return
      } else {
        return
      }
    }

    override fun visitILConditionalExpression(operation: ILConditionalExpression) {
      ProgressIndicatorProvider.checkCanceled()
      operation.getHCLHost() ?: return

      // First check condition
      val condition = operation.condition
      val type = getType(condition)
      if (type == Types.Boolean) {
        // Ok
      } else if (type == Types.String) {
        // Semi ok
        if (condition is ILLiteralExpression && condition.doubleQuotedString != null) {
          if (!GoUtil.isBoolean(condition.unquotedText!!)) {
            holder.registerProblem(condition, "Condition should be boolean or string with boolean value", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
          }
        }
      } else if (type != Types.Any) {
        holder.registerProblem(condition, "Condition should be boolean or string with boolean value", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
      }

      ProgressIndicatorProvider.checkCanceled()

      // Then branches
      val left = operation.then ?: return
      val right = operation.`else` ?: return

      // See TypeCachedValueProvider.doGetType(ILConditionalExpression) for details
      val l = getType(left)
      val r = getType(right)

      ProgressIndicatorProvider.checkCanceled()

      // There's some weird logic in HIL eval_test.go:
      // > // false expression is type-converted to match true expression
      // > // true expression is type-converted to match false expression if the true expression is string
      if (l == r) // Ok
      else if (l == null) // Ok
      else if (r == null) // Ok
      else if (l == Types.Any || r == Types.Any) // Ok
      else if (l == Types.String) // Ok // Would be casted //TODO Check actual value
      else if (r == Types.String) // Ok // Would be casted //TODO Check actual value
      else if (l != r) {
        holder.registerProblem(operation, "Both branches expected to have same type. 'then' is ${l.name}, 'else' is ${r.name}", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
      }

      // TODO: Report if some branch has type Array or Map, they're forbidden for now
    }
  }

  companion object {
    private fun getType(e: ILExpression): Type? {
      return TypeCachedValueProvider.getType(e)
    }
  }

}