// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import org.intellij.plugins.hil.ILCallExpression;

public interface ILMethodCallExpression extends ILExpression, ILCallExpression {

  @NotNull
  ILExpression getILExpression();

  @NotNull
  ILParameterList getILParameterList();

  ILExpression getQualifier();

  @Nullable
  ILVariable getMethod();

  ILParameterList getParameterList();

}
