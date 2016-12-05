// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ILConditionalExpression extends ILExpression {

  @NotNull
  List<ILExpression> getILExpressionList();

  @NotNull
  ILExpression getCondition();

  @Nullable
  ILExpression getThen();

  @Nullable
  ILExpression getElse();

}
