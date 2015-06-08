// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.terraform.il.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.SearchScope;

public interface ILVariable extends ILExpression, PsiNamedElement {

  @NotNull
  PsiElement getId();

  String getName();

  PsiNamedElement setName(String name);

  SearchScope getUseScope();

}
