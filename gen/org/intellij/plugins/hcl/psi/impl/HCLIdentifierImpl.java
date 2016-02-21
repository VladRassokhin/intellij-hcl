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

public class HCLIdentifierImpl extends HCLIdentifierMixin implements HCLIdentifier {

  public HCLIdentifierImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) ((HCLElementVisitor)visitor).visitIdentifier(this);
    else super.accept(visitor);
  }

  @NotNull
  public String getId() {
    return HCLPsiImplUtilJ.getId(this);
  }

}
