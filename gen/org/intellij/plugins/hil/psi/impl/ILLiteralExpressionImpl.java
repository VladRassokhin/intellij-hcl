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
import org.intellij.plugins.hcl.terraform.config.model.Type;

public class ILLiteralExpressionImpl extends ILExpressionWithReference implements ILLiteralExpression {

  public ILLiteralExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ILGeneratedVisitor visitor) {
    visitor.visitILLiteralExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) accept((ILGeneratedVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getDoubleQuotedString() {
    return findChildByType(DOUBLE_QUOTED_STRING);
  }

  @Override
  @Nullable
  public PsiElement getNumber() {
    return findChildByType(NUMBER);
  }

  @Nullable
  public Type getType() {
    return HILPsiImplUtilJ.getType(this);
  }

  @Nullable
  public String getUnquotedText() {
    return HILPsiImplUtilJ.getUnquotedText(this);
  }

}
