// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;

public interface HCLStringLiteral extends HCLLiteral {

  @NotNull
  List<Pair<TextRange, String>> getTextFragments();

  @NotNull
  String getValue();

}
