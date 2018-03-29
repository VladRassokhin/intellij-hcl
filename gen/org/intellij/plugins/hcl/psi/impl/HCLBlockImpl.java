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

public class HCLBlockImpl extends HCLBlockMixin implements HCLBlock {

  public HCLBlockImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitBlock(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @NotNull
  public String getName() {
    return HCLPsiImplUtilJ.getName(this);
  }

  @NotNull
  public String getFullName() {
    return HCLPsiImplUtilJ.getFullName(this);
  }

  @NotNull
  public HCLElement[] getNameElements() {
    return HCLPsiImplUtilJ.getNameElements(this);
  }

  @Nullable
  public HCLObject getObject() {
    return HCLPsiImplUtilJ.getObject(this);
  }

  @Nullable
  public ItemPresentation getPresentation() {
    return HCLPsiImplUtilJ.getPresentation(this);
  }

}
