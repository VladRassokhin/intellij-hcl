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
import org.intellij.plugins.hil.psi.ILBinaryExpression
import org.intellij.plugins.hil.psi.ILElementVisitor
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.getNthChild

abstract class ILBinaryExpressionMixin(node: ASTNode) : ILExpressionImpl(node), ILBinaryExpression {
  override fun getLOperand(): ILExpression {
    return getNthChild(0, ILExpression::class.java)!!
  }


  override fun getROperand(): ILExpression? {
    return getNthChild(2, ILExpression::class.java)
  }

  override fun getOperationSign(): IElementType {
    return node.firstChildNode.treeNext.elementType
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
