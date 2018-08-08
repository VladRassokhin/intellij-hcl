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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;

public class HCLStringLiteralImpl extends HCLStringLiteralMixin implements HCLStringLiteral {

  public HCLStringLiteralImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitStringLiteral(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @NotNull
  public List<Pair<TextRange, String>> getTextFragments() {
    return HCLPsiImplUtilJ.getTextFragments(this);
  }

  @NotNull
  public String getValue() {
    return HCLPsiImplUtilJ.getValue(this);
  }

  public char getQuoteSymbol() {
    return HCLPsiImplUtilJ.getQuoteSymbol(this);
  }

}
