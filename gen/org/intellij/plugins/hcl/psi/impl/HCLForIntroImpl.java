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

public class HCLForIntroImpl extends ASTWrapperPsiElement implements HCLForIntro {

  public HCLForIntroImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitForIntro(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @NotNull
  public HCLIdentifier getVar1() {
    return HCLPsiImplUtilJ.getVar1(this);
  }

  @Nullable
  public HCLIdentifier getVar2() {
    return HCLPsiImplUtilJ.getVar2(this);
  }

  @NotNull
  public HCLExpression getContainer() {
    return HCLPsiImplUtilJ.getContainer(this);
  }

}
