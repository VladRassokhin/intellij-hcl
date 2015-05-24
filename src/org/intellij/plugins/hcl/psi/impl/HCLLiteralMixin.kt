package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import org.intellij.plugins.hcl.psi.HCLLiteral

public abstract class HCLLiteralMixin(node: ASTNode) : HCLElementImpl(node), HCLLiteral {
  private val myRefLock: Any = Any()
  private var myModCount: Long = -1
  private var myRefs: Array<PsiReference> = emptyArray();


  override fun getReferences(): Array<out PsiReference> {
    val count = getManager().getModificationTracker().getModificationCount()
    if (count != myModCount) {
      synchronized(myRefLock) {
        myRefs = ReferenceProvidersRegistry.getReferencesFromProviders(this);
        myModCount = count
      }
    }
    return myRefs
  }
}
