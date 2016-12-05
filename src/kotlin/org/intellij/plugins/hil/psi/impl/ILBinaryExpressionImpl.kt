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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.hil.HILElementType
import org.intellij.plugins.hil.HILTokenType
import org.intellij.plugins.hil.ILBinaryExpression
import org.intellij.plugins.hil.psi.ILElementVisitor
import org.intellij.plugins.hil.psi.ILExpression

open class ILBinaryExpressionImpl(node: ASTNode) : ILExpressionImpl(node), ILBinaryExpression {
  override fun getLOperand(): ILExpression? {
    val nodes = node.getChildren(HILElementType.IL_EXPRESSIONS)
    return (if (nodes.size > 0) nodes[0].psi else null) as ILExpression
  }

  override fun getROperand(): ILExpression? {
    val nodes = node.getChildren(HILElementType.IL_EXPRESSIONS)
    return (if (nodes.size > 2) nodes[2].psi else null) as ILExpression
  }

  override fun getOperationSign(): IElementType? {
    val nodes = node.getChildren(HILTokenType.IL_BINARY_OPERATORS)
    return if (nodes.size == 1) nodes[0].elementType else null
  }

  override fun toString(): String {
    return "ILBinaryExpression"
  }

  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is ILElementVisitor) {
      visitor.visitILBinaryExpression(this)
    } else {
      visitor.visitElement(this)
    }
  }
}
