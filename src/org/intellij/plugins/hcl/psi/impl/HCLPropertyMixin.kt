package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import org.intellij.plugins.hcl.psi.HCLElementGenerator
import org.intellij.plugins.hcl.psi.HCLProperty
import org.jetbrains.annotations.NonNls

abstract class HCLPropertyMixin(node: ASTNode) : HCLElementImpl(node), HCLProperty {

  throws(IncorrectOperationException::class)
  override fun setName(NonNls name: String): PsiElement {
    val generator = HCLElementGenerator(getProject())
    // Strip only both quotes in case user wants some exotic name like key'
    getNameElement().replace(generator.createStringLiteral(StringUtil.unquoteString(name)))
    return this
  }

  override fun getReference(): PsiReference? {
    return HCLPropertyNameReference(this)
  }

  override fun getReferences(): Array<PsiReference> {
    val fromProviders = ReferenceProvidersRegistry.getReferencesFromProviders(this)
    return ArrayUtil.prepend<PsiReference>(HCLPropertyNameReference(this), fromProviders)
  }
}
