// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public interface HCLBinaryExpression extends HCLExpression {

  @NotNull
  HCLExpression getLOperand();

  @Nullable
  HCLExpression getROperand();

  @NotNull
  IElementType getOperationSign();

}
