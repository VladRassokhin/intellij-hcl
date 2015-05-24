package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementGenerator

abstract class HCLBlockMixin(node: ASTNode) : HCLElementImpl(node), HCLBlock {

  throws(IncorrectOperationException::class)
  override fun setName(name: String): PsiElement {
    val generator = HCLElementGenerator(getProject())
    // Strip only both quotes in case user wants some exotic name like key
    // TODO: do something
    //    getNameElement().replace(generator.createStringLiteral(StringUtil.unquoteString(name)));
    return this
  }

  // TODO: Add proper references
}
