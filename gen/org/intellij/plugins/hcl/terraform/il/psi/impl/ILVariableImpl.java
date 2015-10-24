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
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;

public class ILVariableImpl extends ILExpressionWithReference implements ILVariable {

  public ILVariableImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ILGeneratedVisitor) ((ILGeneratedVisitor)visitor).visitILVariable(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getId() {
    return findChildByType(ID);
  }

  @NotNull
  public String getName() {
    return TILPsiImplUtilJ.getName(this);
  }

  public PsiNamedElement setName(String name) {
    return TILPsiImplUtilJ.setName(this, name);
  }

  @NotNull
  public SearchScope getUseScope() {
    return TILPsiImplUtilJ.getUseScope(this);
  }

  @NotNull
  public GlobalSearchScope getResolveScope() {
    return TILPsiImplUtilJ.getResolveScope(this);
  }

}
