package org.intellij.plugins.hcl.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil
import org.intellij.plugins.hcl.psi.HCLElement

/**
 * @author Mikhail Golubev
 */
public open class HCLElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), HCLElement {

  override fun toString(): String {
    val className = javaClass.getSimpleName()
    return StringUtil.trimEnd(className, "Impl")
  }
}
