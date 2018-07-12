// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.intellij.plugins.hcl.psi.*;

public class HCLFullSplatImpl extends ASTWrapperPsiElement implements HCLFullSplat {

  public HCLFullSplatImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitFullSplat(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HCLGetAttr> getGetAttrList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HCLGetAttr.class);
  }

  @Override
  @NotNull
  public List<HCLIndex> getIndexList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HCLIndex.class);
  }

}
