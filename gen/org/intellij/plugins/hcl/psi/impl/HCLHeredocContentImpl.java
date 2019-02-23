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

public class HCLHeredocContentImpl extends HCLHeredocContentMixin implements HCLHeredocContent {

  public HCLHeredocContentImpl(@Nullable ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitHeredocContent(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @NotNull
  public List<String> getLines() {
    return HCLPsiImplUtilJ.getLines(this);
  }

  @NotNull
  public List<CharSequence> getLinesRaw() {
    return HCLPsiImplUtilJ.getLinesRaw(this);
  }

  public int getLinesCount() {
    return HCLPsiImplUtilJ.getLinesCount(this);
  }

  @NotNull
  public String getValue() {
    return HCLPsiImplUtilJ.getValue(this);
  }

  @Nullable
  public Integer getMinimalIndentation() {
    return HCLPsiImplUtilJ.getMinimalIndentation(this);
  }

  @NotNull
  public List<Pair<TextRange, String>> getTextFragments() {
    return HCLPsiImplUtilJ.getTextFragments(this);
  }

}
