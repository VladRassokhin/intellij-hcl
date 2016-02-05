// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;

public interface ILVariable extends ILExpression, PsiNamedElement {

  @Nullable
  PsiElement getId();

  @NotNull
  String getName();

  PsiNamedElement setName(String name);

  @NotNull
  SearchScope getUseScope();

  @NotNull
  GlobalSearchScope getResolveScope();

}
