// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public interface ILBinaryExpression extends ILExpression {

  @NotNull
  ILExpression getLOperand();

  @Nullable
  ILExpression getROperand();

  @NotNull
  IElementType getOperationSign();

}
