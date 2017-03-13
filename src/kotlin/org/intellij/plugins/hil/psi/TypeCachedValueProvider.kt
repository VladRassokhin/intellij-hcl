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
package org.intellij.plugins.hil.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hil.HILElementTypes
import org.intellij.plugins.hil.HILTypes.ILBinaryBooleanOnlyOperations

class TypeCachedValueProvider private constructor(private val e: ILExpression) : CachedValueProvider<Type?> {

  companion object {
    fun getType(e: ILExpression): Type? {
      return CachedValuesManager.getCachedValue(e, TypeCachedValueProvider(e))
    }

    private val LOG = Logger.getInstance(TypeCachedValueProvider::class.java)

    private fun doGetType(e: ILExpressionHolder): Type? {
      val expression = e.ilExpression ?: return Types.Any
      return getType(expression)
    }

    private fun doGetType(e: ILParenthesizedExpression): Type? {
      val expression = e.ilExpression ?: return Types.Any
      return getType(expression)
    }

    private fun doGetType(e: ILLiteralExpression): Type? {
      // TODO: Don't use `ILLiteralExpression#getType` cause it would cause SO
      return when {
        e.doubleQuotedString != null -> Types.String
        e.number != null -> Types.Number
        "true".equals(e.text, true) -> Types.Boolean
        "false".equals(e.text, true) -> Types.Boolean
        else -> null
      }
    }

    private fun doGetType(e: ILUnaryExpression): Type? {
      val sign = e.operationSign
      if (sign == HILElementTypes.OP_PLUS || sign == HILElementTypes.OP_MINUS) {
        return Types.Number
      } else if (sign == HILElementTypes.OP_NOT) {
        return Types.Boolean
      } else {
        LOG.error("Unexpected operation sign of UnaryExpression: $sign", e.text)
        return null
      }
    }

    private fun doGetType(e: ILBinaryExpression): Type? {
      val et = e.node.elementType
      if (et == HILElementTypes.IL_BINARY_ADDITION_EXPRESSION || et == HILElementTypes.IL_BINARY_MULTIPLY_EXPRESSION) {
        return Types.Number
      } else if (et == HILElementTypes.IL_BINARY_RELATIONAL_EXPRESSION || et == HILElementTypes.IL_BINARY_EQUALITY_EXPRESSION) {
        return Types.Boolean
      } else if (et in ILBinaryBooleanOnlyOperations) {
        return Types.Boolean
      }
      return null
    }

    private fun doGetType(e: ILConditionalExpression): Type? {
      val first = e.then
      val second = e.`else`
      val l = first?.let { getType(it) }
      val r = second?.let { getType(it) }

      // There's some weird logic in HIL eval_test.go:
      // > // false expression is type-converted to match true expression
      // > // true expression is type-converted to match false expression if the true expression is string
      if (l == r) return l
      if (l == null) return r
      if (r == null) return l
      if (l == Types.Any || r == Types.Any) return Types.Any
      if (l == Types.String) return r
      return l
    }
  }


  override fun compute(): CachedValueProvider.Result<Type?>? {
    return when (e) {
      is ILExpressionHolder -> doGetType(e)?.let { CachedValueProvider.Result.create(it, e) }
      is ILParenthesizedExpression -> doGetType(e)?.let { CachedValueProvider.Result.create(it, e) }

      is ILLiteralExpression -> doGetType(e)?.let { CachedValueProvider.Result.create(it, e) }

      is ILUnaryExpression -> doGetType(e)?.let { CachedValueProvider.Result.create(it, e) }
      is ILBinaryExpression -> doGetType(e)?.let { CachedValueProvider.Result.create(it, e) }
      is ILConditionalExpression -> doGetType(e)?.let { CachedValueProvider.Result.create(it, e) }

    // TODO: Implement via resolving/model :
      is ILVariable -> null
      is ILSelectExpression -> null
      is ILMethodCallExpression -> {
        val method = e.method?.name
        if (method != null && e.callee === e.method) {
          TypeModelProvider.getModel(e.project).functions.firstOrNull { it.name == method }?.ret?.let { CachedValueProvider.Result.create(it, e.method) }
        } else null
      }

    // Errors:
      is ILParameterList -> {
        LOG.error("#getType should not be called for ILParameterList", e.text)
        return null
      }
      else -> {
        LOG.error("Unexpected #getType call for ${e.javaClass.name}", e.text)
        return null
      }
    }
  }

}