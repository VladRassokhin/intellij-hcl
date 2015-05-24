package org.intellij.plugins.hcl.psi.impl

import com.intellij.json.psi.impl.JSStringLiteralEscaper
import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.LeafElement
import org.intellij.plugins.hcl.psi.HCLLiteral

public abstract class HCLStringLiteralMixin(node: ASTNode?) : HCLLiteralImpl(node), PsiLanguageInjectionHost {
  override fun isValidHost() = true

  override fun updateText(text: String): PsiLanguageInjectionHost {
    val vNode = getNode().getFirstChildNode() as LeafElement
    vNode.replaceWithText(text)
    return this
  }

  override fun createLiteralTextEscaper() = object: JSStringLiteralEscaper<PsiLanguageInjectionHost>(this) {
    override fun isRegExpLiteral() = true
  }
}
