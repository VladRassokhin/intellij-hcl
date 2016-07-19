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

public class ILParameterListImpl extends ILExpressionBase implements ILParameterList {

  public ILParameterListImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ILGeneratedVisitor visitor) {
    visitor.visitILParameterList(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) accept((ILGeneratedVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<ILExpression> getParametersList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ILExpression.class);
  }

}
