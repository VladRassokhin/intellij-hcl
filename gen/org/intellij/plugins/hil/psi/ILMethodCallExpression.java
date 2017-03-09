// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ILMethodCallExpression extends ILExpression {

  @NotNull
  ILExpression getCallee();

  @NotNull
  ILParameterList getParameterList();

  ILExpression getQualifier();

  @Nullable
  ILVariable getMethod();

}
