// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HCLForExpression extends HCLExpression {

  @Nullable
  HCLExpression getExpression();

  @NotNull
  HCLForIntro getIntro();

  @Nullable
  HCLForCondition getCondition();

}
