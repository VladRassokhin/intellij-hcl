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

public class ILConditionalExpressionImpl extends ILExpressionImpl implements ILConditionalExpression {

  public ILConditionalExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ILGeneratedVisitor visitor) {
    visitor.visitILConditionalExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) accept((ILGeneratedVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<ILExpression> getILExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ILExpression.class);
  }

  @Override
  @NotNull
  public ILExpression getCondition() {
    List<ILExpression> p1 = getILExpressionList();
    return p1.get(0);
  }

  @Override
  @Nullable
  public ILExpression getThen() {
    List<ILExpression> p1 = getILExpressionList();
    return p1.size() < 2 ? null : p1.get(1);
  }

  @Override
  @Nullable
  public ILExpression getElse() {
    List<ILExpression> p1 = getILExpressionList();
    return p1.size() < 3 ? null : p1.get(2);
  }

}
