// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiNameIdentifierOwner;

public class HCLElementVisitor extends PsiElementVisitor {

  public void visitBinaryAdditionExpression(@NotNull HCLBinaryAdditionExpression o) {
    visitBinaryExpression(o);
  }

  public void visitBinaryAndExpression(@NotNull HCLBinaryAndExpression o) {
    visitBinaryExpression(o);
  }

  public void visitBinaryEqualityExpression(@NotNull HCLBinaryEqualityExpression o) {
    visitBinaryExpression(o);
  }

  public void visitBinaryExpression(@NotNull HCLBinaryExpression o) {
    visitExpression(o);
  }

  public void visitBinaryMultiplyExpression(@NotNull HCLBinaryMultiplyExpression o) {
    visitBinaryExpression(o);
  }

  public void visitBinaryOrExpression(@NotNull HCLBinaryOrExpression o) {
    visitBinaryExpression(o);
  }

  public void visitBinaryRelationalExpression(@NotNull HCLBinaryRelationalExpression o) {
    visitBinaryExpression(o);
  }

  public void visitCollectionValue(@NotNull HCLCollectionValue o) {
    visitExpression(o);
  }

  public void visitConditionalExpression(@NotNull HCLConditionalExpression o) {
    visitExpression(o);
  }

  public void visitExpression(@NotNull HCLExpression o) {
    visitPsiElement(o);
  }

  public void visitForArrayExpression(@NotNull HCLForArrayExpression o) {
    visitForExpression(o);
  }

  public void visitForCondition(@NotNull HCLForCondition o) {
    visitPsiElement(o);
  }

  public void visitForExpression(@NotNull HCLForExpression o) {
    visitExpression(o);
  }

  public void visitForIntro(@NotNull HCLForIntro o) {
    visitPsiElement(o);
  }

  public void visitForObjectExpression(@NotNull HCLForObjectExpression o) {
    visitForExpression(o);
  }

  public void visitIndexSelectExpression(@NotNull HCLIndexSelectExpression o) {
    visitSelectExpression(o);
  }

  public void visitMethodCallExpression(@NotNull HCLMethodCallExpression o) {
    visitExpression(o);
  }

  public void visitParameterList(@NotNull HCLParameterList o) {
    visitPsiElement(o);
  }

  public void visitParenthesizedExpression(@NotNull HCLParenthesizedExpression o) {
    visitExpression(o);
  }

  public void visitSelectExpression(@NotNull HCLSelectExpression o) {
    visitExpression(o);
  }

  public void visitSplatSelectExpression(@NotNull HCLSplatSelectExpression o) {
    visitSelectExpression(o);
  }

  public void visitTemplate(@NotNull HCLTemplate o) {
    visitPsiElement(o);
  }

  public void visitTemplateDirective(@NotNull HCLTemplateDirective o) {
    visitPsiElement(o);
  }

  public void visitTemplateExpression(@NotNull HCLTemplateExpression o) {
    visitExpression(o);
  }

  public void visitTemplateFor(@NotNull HCLTemplateFor o) {
    visitPsiElement(o);
  }

  public void visitTemplateIf(@NotNull HCLTemplateIf o) {
    visitPsiElement(o);
  }

  public void visitTemplateInterpolation(@NotNull HCLTemplateInterpolation o) {
    visitPsiElement(o);
  }

  public void visitUnaryExpression(@NotNull HCLUnaryExpression o) {
    visitExpression(o);
  }

  public void visitVariable(@NotNull HCLVariable o) {
    visitExpression(o);
    // visitPsiNamedElement(o);
  }

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

  public void visitObject2(@NotNull HCLObject2 o) {
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
    visitExpression(o);
    // visitElement(o);
  }

  public void visitElement(@NotNull HCLElement o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
