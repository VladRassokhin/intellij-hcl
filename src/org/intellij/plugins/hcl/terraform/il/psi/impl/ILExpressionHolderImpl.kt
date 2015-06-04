package org.intellij.plugins.hcl.terraform.il.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import org.intellij.plugins.hcl.terraform.il.psi.ILExpression
import org.intellij.plugins.hcl.terraform.il.psi.ILExpressionHolder

public class ILExpressionHolderImpl(node: ASTNode) : ILExpressionBase(node), ILExpressionHolder {

  override fun getLanguage(): Language {
    return getNode().getElementType().getLanguage()
  }

  override fun getILExpression(): ILExpression? {
    return findChildByClass(javaClass<ILExpression>())
  }

}
