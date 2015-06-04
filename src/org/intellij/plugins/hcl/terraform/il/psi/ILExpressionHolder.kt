package org.intellij.plugins.hcl.terraform.il.psi

public interface ILExpressionHolder : ILExpression {

  public fun getILExpression(): ILExpression?
}
