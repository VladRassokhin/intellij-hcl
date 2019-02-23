// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;

public interface HCLObject extends HCLContainer {

  @NotNull
  List<HCLBlock> getBlockList();

  @NotNull
  List<HCLProperty> getPropertyList();

  @Nullable
  HCLProperty findProperty(@NotNull String name);

  @Nullable
  ItemPresentation getPresentation();

}
