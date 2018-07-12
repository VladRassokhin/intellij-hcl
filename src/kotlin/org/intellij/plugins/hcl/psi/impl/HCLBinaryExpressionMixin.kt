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
package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.hcl.psi.HCLBinaryExpression
import org.intellij.plugins.hcl.psi.HCLElementVisitor
import org.intellij.plugins.hcl.psi.HCLExpression
import org.intellij.plugins.hil.psi.getNthChild

abstract class HCLBinaryExpressionMixin(node: ASTNode) : HCLExpressionImpl(node), HCLBinaryExpression {
  override fun getLOperand(): HCLExpression {
    return getNthChild(0, HCLExpression::class.java)!!
  }


  override fun getROperand(): HCLExpression? {
    return getNthChild(2, HCLExpression::class.java)
  }

  override fun getOperationSign(): IElementType {
    return node.firstChildNode.treeNext.elementType
  }

  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is HCLElementVisitor) {
      visitor.visitBinaryExpression(this)
    } else {
      visitor.visitElement(this)
    }
  }
}
