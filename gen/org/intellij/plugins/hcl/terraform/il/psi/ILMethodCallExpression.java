// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.terraform.il.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import org.intellij.plugins.hcl.terraform.il.ILCallExpression;

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
