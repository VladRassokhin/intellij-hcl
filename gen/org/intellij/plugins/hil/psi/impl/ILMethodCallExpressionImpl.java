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

public class ILMethodCallExpressionImpl extends ILExpressionImpl implements ILMethodCallExpression {

  public ILMethodCallExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) ((ILGeneratedVisitor)visitor).visitILMethodCallExpression(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public ILExpression getExpression() {
    return findNotNullChildByClass(ILExpression.class);
  }

  @Override
  @NotNull
  public ILParameterList getParameterList() {
    return findNotNullChildByClass(ILParameterList.class);
  }

  public ILExpression getQualifier() {
    return HILPsiImplUtilJ.getQualifier(this);
  }

  @Nullable
  public ILVariable getMethod() {
    return HILPsiImplUtilJ.getMethod(this);
  }

}
