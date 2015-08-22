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

public class HCLHeredocLiteralImpl extends HCLStringLiteralMixin implements HCLHeredocLiteral {

  public HCLHeredocLiteralImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) ((HCLElementVisitor)visitor).visitHeredocLiteral(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public HCLHeredocContent getHeredocContent() {
    return findNotNullChildByClass(HCLHeredocContent.class);
  }

  @Override
  @NotNull
  public List<HCLHeredocMarker> getHeredocMarkerList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HCLHeredocMarker.class);
  }

  @NotNull
  public String getValue() {
    return HCLPsiImplUtilJ.getValue(this);
  }

}
