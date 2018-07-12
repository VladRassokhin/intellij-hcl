// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HCLTemplate extends PsiElement {

  @NotNull
  List<HCLExpression> getExpressionList();

  @NotNull
  List<HCLTemplateDirective> getTemplateDirectiveList();

  @NotNull
  List<HCLTemplateInterpolation> getTemplateInterpolationList();

}
