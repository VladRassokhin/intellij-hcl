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
import com.intellij.psi.tree.IElementType;

public class HCLBinaryExpressionImpl extends HCLBinaryExpressionMixin implements HCLBinaryExpression {

  public HCLBinaryExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitBinaryExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public HCLExpression getLOperand() {
    List<HCLExpression> p1 = PsiTreeUtil.getChildrenOfTypeAsList(this, HCLExpression.class);
    return p1.get(0);
  }

  @Override
  @Nullable
  public HCLExpression getROperand() {
    List<HCLExpression> p1 = PsiTreeUtil.getChildrenOfTypeAsList(this, HCLExpression.class);
    return p1.size() < 2 ? null : p1.get(1);
  }

}
