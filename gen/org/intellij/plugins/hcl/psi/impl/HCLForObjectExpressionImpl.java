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

public class HCLForObjectExpressionImpl extends HCLForExpressionImpl implements HCLForObjectExpression {

  public HCLForObjectExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitForObjectExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @NotNull
  public HCLExpression getKey() {
    return HCLPsiImplUtilJ.getKey(this);
  }

  @NotNull
  public HCLExpression getValue() {
    return HCLPsiImplUtilJ.getValue(this);
  }

  public boolean isGrouping() {
    return HCLPsiImplUtilJ.isGrouping(this);
  }

}
