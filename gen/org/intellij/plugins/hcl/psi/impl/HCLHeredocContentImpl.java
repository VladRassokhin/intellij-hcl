// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.intellij.plugins.hcl.psi.*;

public class HCLHeredocContentImpl extends ASTWrapperPsiElement implements HCLHeredocContent {

  public HCLHeredocContentImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) ((HCLElementVisitor)visitor).visitHeredocContent(this);
    else super.accept(visitor);
  }

}
