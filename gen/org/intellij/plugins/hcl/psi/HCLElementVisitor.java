// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;

public class HCLElementVisitor extends PsiElementVisitor {

  public void visitArray(@NotNull HCLArray o) {
    visitContainer(o);
  }

  public void visitBlock(@NotNull HCLBlock o) {
    visitElement(o);
    // visitPsiNameIdentifierOwner(o);
  }

  public void visitBooleanLiteral(@NotNull HCLBooleanLiteral o) {
    visitLiteral(o);
  }

  public void visitContainer(@NotNull HCLContainer o) {
    visitValue(o);
  }

  public void visitHeredocContent(@NotNull HCLHeredocContent o) {
    visitPsiElement(o);
  }

  public void visitHeredocLiteral(@NotNull HCLHeredocLiteral o) {
    visitLiteral(o);
  }

  public void visitHeredocMarker(@NotNull HCLHeredocMarker o) {
    visitPsiElement(o);
  }

  public void visitIdentifier(@NotNull HCLIdentifier o) {
    visitValue(o);
  }

  public void visitLiteral(@NotNull HCLLiteral o) {
    visitValue(o);
  }

  public void visitNullLiteral(@NotNull HCLNullLiteral o) {
    visitLiteral(o);
  }

  public void visitNumberLiteral(@NotNull HCLNumberLiteral o) {
    visitLiteral(o);
  }

  public void visitObject(@NotNull HCLObject o) {
    visitContainer(o);
  }

  public void visitProperty(@NotNull HCLProperty o) {
    visitElement(o);
    // visitPsiNameIdentifierOwner(o);
  }

  public void visitStringLiteral(@NotNull HCLStringLiteral o) {
    visitLiteral(o);
  }

  public void visitValue(@NotNull HCLValue o) {
    visitElement(o);
  }

  public void visitElement(@NotNull HCLElement o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
