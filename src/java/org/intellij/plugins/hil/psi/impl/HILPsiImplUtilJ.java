/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.hil.psi.impl;

import com.intellij.psi.tree.IElementType;
import org.intellij.plugins.hil.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HILPsiImplUtilJ {
  public static Class getTypeClass(ILLiteralExpression expression) {
    return HILPsiImplUtils.INSTANCE.getTypeClass(expression);
  }

  public static ILExpression getQualifier(ILMethodCallExpression expression) {
    return HILPsiImplUtils.INSTANCE.getQualifier(expression);
  }

  @Nullable
  public static ILVariable getMethod(ILMethodCallExpression expression) {
    return HILPsiImplUtils.INSTANCE.getMethod(expression);
  }

  @Nullable
  public static String getUnquotedText(ILLiteralExpression literal){
    return HILPsiImplUtils.INSTANCE.getUnquotedText(literal);
  }

  @NotNull
  public static IElementType getOperationSign(ILUnaryExpression expression) {
    return HILPsiImplUtils.INSTANCE.getOperationSign(expression);
  }

  @NotNull
  public static IElementType getOperationSign(ILBinaryExpression expression) {
    assert expression instanceof ILBinaryExpressionMixin;
    return null;
  }
}
