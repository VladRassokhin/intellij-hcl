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
package org.intellij.plugins.hil.psi.impl

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.intellij.plugins.hcl.terraform.config.model.getTerraformSearchScope
import org.intellij.plugins.hil.psi.*

object HILPsiImplUtils {
  fun getTypeClass(expression: ILLiteralExpressionImpl): Class<*> {
    // TODO use type classes from HIL model
    return String::class.java;
  }

  fun getParameters(list: ILParameterList): Array<ILExpression> {
    val expressions = PsiTreeUtil.getChildrenOfTypeAsList(list, ILExpression::class.java)
    return expressions.toTypedArray()
  }

  fun getQualifier(expression: ILMethodCallExpression): ILExpression? {
    val select = expression.ilExpression
    return if (select === expression.getMethod()) null else select
  }

  fun getMethod(expression: ILMethodCallExpression): ILVariable? {
    val sibling = expression.ilParameterList.prevSibling
    if (sibling is ILVariable) {
      return sibling
    }
    return null
  }

  fun getParameterList(expression: ILMethodCallExpressionImpl): ILParameterList {
    return expression.ilParameterList
  }


  fun getName(variable: ILVariableImpl): String {
    return variable.text
  }

  @Throws(IncorrectOperationException::class)
  fun setName(variable: ILVariableImpl, name: String): PsiNamedElement {
    val node = variable.firstChild.node
    assert(node is LeafElement)
    (node as LeafElement).replaceWithText(name)
    return variable
  }

  fun getUseScope(variable: ILVariableImpl): SearchScope {
    val host = InjectedLanguageManager.getInstance(variable.project).getInjectionHost(variable)
    if (host != null) {
      return host.getTerraformSearchScope()
    } else {
      // Fallback
      return variable.getTerraformSearchScope();
    };
  }

  fun getResolveScope(variable: ILVariableImpl): GlobalSearchScope {
    val host = InjectedLanguageManager.getInstance(variable.project).getInjectionHost(variable)
    if (host != null) {
      return host.getTerraformSearchScope()
    } else {
      // Fallback
      return variable.getTerraformSearchScope();
    };
  }

  fun createVariable(name: String, project: Project): ILVariable? {
    val generator = ILElementGenerator(project)
    return generator.createILVariable(name)
  }

  fun getField(expression: ILSelectExpression): ILExpression? {
    val list = PsiTreeUtil.getChildrenOfTypeAsList(expression, ILExpression::class.java)
    return list.getOrNull(1)
  }

  fun getUnquotedText(literal: ILLiteralExpression): String? {
    val dqs = literal.doubleQuotedString
    if (dqs != null) {
      return StringUtil.unquoteString(dqs.text)
    }
    return literal.text
  }
}
