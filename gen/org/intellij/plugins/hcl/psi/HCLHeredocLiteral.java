// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HCLHeredocLiteral extends HCLLiteral {

  @NotNull
  HCLHeredocContent getHeredocContent();

  @NotNull
  List<HCLHeredocMarker> getHeredocMarkerList();

  @NotNull
  String getValue();

}
