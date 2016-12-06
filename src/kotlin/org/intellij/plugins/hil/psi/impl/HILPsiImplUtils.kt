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
package org.intellij.plugins.hil.psi.impl

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hil.HILTokenTypes
import org.intellij.plugins.hil.psi.*

object HILPsiImplUtils {
  fun getType(e: ILExpression): Type? {
    return TypeCachedValueProvider.getType(e)
  }

  fun getQualifier(expression: ILMethodCallExpression): ILExpression? {
    val select = expression.expression
    return if (select === expression.method) null else select
  }

  fun getMethod(expression: ILMethodCallExpression): ILVariable? {
    val sibling = expression.parameterList.prevSibling
    if (sibling is ILVariable) {
      return sibling
    }
    return null
  }


  fun getName(variable: ILVariable): String {
    return variable.text
  }

  fun getUnquotedText(literal: ILLiteralExpression): String? {
    val dqs = literal.doubleQuotedString
    if (dqs != null) {
      return StringUtil.unquoteString(dqs.text)
    }
    return literal.text
  }

  fun getOperationSign(expression: ILUnaryExpression): IElementType {
    val nodes = expression.node.getChildren(HILTokenTypes.IL_UNARY_OPERATORS)
    return nodes.first().elementType
  }
}
