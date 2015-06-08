// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.terraform.il.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hcl.terraform.il.TILElementTypes.*;
import org.intellij.plugins.hcl.terraform.il.psi.*;

public class ILParenthesizedExpressionImpl extends ILExpressionImpl implements ILParenthesizedExpression {

  public ILParenthesizedExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) ((ILGeneratedVisitor)visitor).visitILParenthesizedExpression(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public ILExpression getILExpression() {
    return findChildByClass(ILExpression.class);
  }

}
