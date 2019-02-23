// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hil.HILElementTypes.*;
import org.intellij.plugins.hil.psi.*;
import com.intellij.psi.tree.IElementType;

public class ILBinaryExpressionImpl extends ILBinaryExpressionMixin implements ILBinaryExpression {

  public ILBinaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ILGeneratedVisitor visitor) {
    visitor.visitILBinaryExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) accept((ILGeneratedVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public ILExpression getLOperand() {
    List<ILExpression> p1 = PsiTreeUtil.getChildrenOfTypeAsList(this, ILExpression.class);
    return p1.get(0);
  }

  @Override
  @Nullable
  public ILExpression getROperand() {
    List<ILExpression> p1 = PsiTreeUtil.getChildrenOfTypeAsList(this, ILExpression.class);
    return p1.size() < 2 ? null : p1.get(1);
  }

  @NotNull
  public IElementType getOperationSign() {
    return HILPsiImplUtilJ.getOperationSign(this);
  }

}
