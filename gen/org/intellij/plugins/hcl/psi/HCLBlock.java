// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.navigation.ItemPresentation;

public interface HCLBlock extends HCLElement, PsiNamedElement {

  @NotNull
  String getName();

  @NotNull
  HCLElement[] getNameElements();

  @Nullable
  HCLValue getObject();

  @Nullable
  ItemPresentation getPresentation();

}
