// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import org.intellij.plugins.hcl.psi.*;
import com.intellij.navigation.ItemPresentation;

public class HCLObjectImpl extends HCLContainerImpl implements HCLObject {

  public HCLObjectImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitObject(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HCLBlock> getBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HCLBlock.class);
  }

  @Override
  @NotNull
  public List<HCLProperty> getPropertyList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HCLProperty.class);
  }

  @Nullable
  public HCLProperty findProperty(String name) {
    return HCLPsiImplUtilJ.findProperty(this, name);
  }

  @Nullable
  public ItemPresentation getPresentation() {
    return HCLPsiImplUtilJ.getPresentation(this);
  }

}
