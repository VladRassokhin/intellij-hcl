// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HCLConditionalExpression extends HCLExpression {

  @NotNull
  List<HCLExpression> getExpressionList();

  @NotNull
  HCLExpression getCondition();

  @Nullable
  HCLExpression getThen();

  @Nullable
  HCLExpression getElse();

}
