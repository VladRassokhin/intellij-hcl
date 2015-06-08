/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.il.psi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.intellij.plugins.hcl.terraform.il.TILLanguage;
import org.intellij.plugins.hcl.terraform.il.psi.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TILPsiImplUtilJ {
  public static Class getTypeClass(ILLiteralExpressionImpl expression) {
    return String.class;
  }

  public static ILExpression[] getParameters(ILParameterList list) {
    final List<ILExpression> expressions = list.getILExpressionList();
    return expressions.toArray(new ILExpression[expressions.size()]);
  }

  public static ILExpression getQualifier(ILMethodCallExpression expression) {
    ILExpression select = expression.getILExpression();
    return select == expression.getMethod() ? null : select;
  }

  @Nullable
  public static ILVariable getMethod(ILMethodCallExpression expression) {
    PsiElement sibling = expression.getILParameterList().getPrevSibling();
    if (sibling instanceof ILVariable) {
      return (ILVariable) sibling;
    }
    return null;
  }

  public static ILParameterList getParameterList(ILMethodCallExpressionImpl expression) {
    return expression.getILParameterList();
  }


  public static String getName(ILVariableImpl variable) {
    return variable.getText();
  }

  public static PsiNamedElement setName(ILVariableImpl variable, String name) throws IncorrectOperationException {
    ILVariable newElement = createVariable(name, variable.getProject());
    if (newElement == null) throw new IncorrectOperationException("Cannot create variable with name '" + name + "'");
    variable.replace(newElement);
    return newElement;
  }

  public static SearchScope getUseScope(ILVariableImpl variable) {
    return new LocalSearchScope(variable.getContainingFile());
  }

  @Nullable
  public static ILVariable createVariable(String name, Project project) {
    PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(TILLanguage.INSTANCE$, "${" + name + "}");
    return (ILVariable) file.getFirstChild().getChildren()[0];
  }

  public static ILVariable getField(ILSelectExpression expression) {
    List<? extends ILExpression> list = expression.getILExpressionList();
    return list.size() < 2 ? null : (ILVariable)list.get(1);
  }
}
