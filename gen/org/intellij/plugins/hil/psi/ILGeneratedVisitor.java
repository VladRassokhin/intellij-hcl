// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hil.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.PsiNamedElement;

public class ILGeneratedVisitor extends PsiElementVisitor {

  public void visitILBinaryAdditionExpression(@NotNull ILBinaryAdditionExpression o) {
    visitILBinaryExpression(o);
  }

  public void visitILBinaryAndExpression(@NotNull ILBinaryAndExpression o) {
    visitILBinaryExpression(o);
  }

  public void visitILBinaryEqualityExpression(@NotNull ILBinaryEqualityExpression o) {
    visitILBinaryExpression(o);
  }

  public void visitILBinaryExpression(@NotNull ILBinaryExpression o) {
    visitILExpression(o);
  }

  public void visitILBinaryMultiplyExpression(@NotNull ILBinaryMultiplyExpression o) {
    visitILBinaryExpression(o);
  }

  public void visitILBinaryOrExpression(@NotNull ILBinaryOrExpression o) {
    visitILBinaryExpression(o);
  }

  public void visitILBinaryRelationalExpression(@NotNull ILBinaryRelationalExpression o) {
    visitILBinaryExpression(o);
  }

  public void visitILConditionalExpression(@NotNull ILConditionalExpression o) {
    visitILExpression(o);
  }

  public void visitILExpression(@NotNull ILExpression o) {
    visitOuterLanguageElement(o);
  }

  public void visitILExpressionHolder(@NotNull ILExpressionHolder o) {
    visitILExpression(o);
  }

  public void visitILIndexSelectExpression(@NotNull ILIndexSelectExpression o) {
    visitILSelectExpression(o);
  }

  public void visitILLiteralExpression(@NotNull ILLiteralExpression o) {
    visitILExpression(o);
  }

  public void visitILMethodCallExpression(@NotNull ILMethodCallExpression o) {
    visitILExpression(o);
  }

  public void visitILParameterList(@NotNull ILParameterList o) {
    visitILExpression(o);
  }

  public void visitILParenthesizedExpression(@NotNull ILParenthesizedExpression o) {
    visitILExpression(o);
  }

  public void visitILSelectExpression(@NotNull ILSelectExpression o) {
    visitILExpression(o);
  }

  public void visitILUnaryExpression(@NotNull ILUnaryExpression o) {
    visitILExpression(o);
  }

  public void visitILVariable(@NotNull ILVariable o) {
    visitILExpression(o);
    // visitPsiNamedElement(o);
  }

  public void visitOuterLanguageElement(@NotNull OuterLanguageElement o) {
    visitElement(o);
  }

}
