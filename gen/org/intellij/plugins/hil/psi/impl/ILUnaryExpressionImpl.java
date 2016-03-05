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

public class ILUnaryExpressionImpl extends ILExpressionImpl implements ILUnaryExpression {

  public ILUnaryExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) ((ILGeneratedVisitor)visitor).visitILUnaryExpression(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public ILExpression getOperand() {
    return findChildByClass(ILExpression.class);
  }

  @NotNull
  public IElementType getOperationSign() {
    return HILPsiImplUtilJ.getOperationSign(this);
  }

}
