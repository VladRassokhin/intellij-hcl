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
package org.intellij.plugins.hcl.terraform.il.codeinsight

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.icons.AllIcons
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import getPrevSiblingNonWhiteSpace
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.terraform.config.model.Function
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hcl.terraform.config.model.Variable
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.il.TILLanguage
import org.intellij.plugins.hcl.terraform.il.psi.ILSelectExpression
import org.intellij.plugins.hcl.terraform.il.psi.ILVariable
import java.util.*

public class TILCompletionProvider : CompletionContributor() {
  init {
    val VAR_SELECT = PlatformPatterns.psiElement(ILSelectExpression::class.java).with(object : PatternCondition<ILSelectExpression?>("SelectFromVar") {
      override fun accepts(t: ILSelectExpression?, context: ProcessingContext?): Boolean {
        val from = t?.from
        return from is ILVariable && from.name == "var"
      }
    })

    extend(CompletionType.BASIC, METHOD_POSITION, MethodsCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(TILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, VAR_SELECT)
        , VariableCompletionProvider)
  }

  companion object {
    @JvmField public val TERRAFORM_METHODS: SortedSet<String> = ServiceManager.getService(TypeModelProvider::class.java).get().functions.map { it.name }.toSortedSet()
    @JvmField public val GLOBAL_SCOPES: SortedSet<String> = sortedSetOf("var", "self", "path")
    @JvmField public val FUNCTIONS = ServiceManager.getService(TypeModelProvider::class.java).get().functions
    private val METHOD_POSITION = PlatformPatterns.psiElement().withLanguage(TILLanguage)
        .withParent(ILVariable::class.java)
        .andNot(PlatformPatterns.psiElement().withSuperParent(2, ILSelectExpression::class.java))

    private val LOG = Logger.getInstance(TILCompletionProvider::class.java)
    fun create(value: String): LookupElementBuilder {
      var builder = LookupElementBuilder.create(value)
      return builder
    }

    fun createScope(value: String): LookupElementBuilder {
      var builder = LookupElementBuilder.create(value)
      builder = builder.withInsertHandler(ScopeSelectInsertHandler)
      builder = builder.withRenderer(object : LookupElementRenderer<LookupElement?>() {
        override fun renderElement(element: LookupElement?, presentation: LookupElementPresentation?) {
          presentation?.icon = AllIcons.Nodes.Advice
          presentation?.itemText = element?.lookupString
        }
      })
      return builder
    }

    fun create(f: Function): LookupElementBuilder {
      var builder = LookupElementBuilder.create(f.name)
      builder = builder.withInsertHandler(FunctionInsertHandler)
      builder = builder.withRenderer(object : LookupElementRenderer<LookupElement?>() {
        override fun renderElement(element: LookupElement?, presentation: LookupElementPresentation?) {
          presentation?.icon = AllIcons.Nodes.Method // or Function
          presentation?.itemText = element?.lookupString
        }
      })
      return builder
    }
  }

  private object MethodsCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug("TIL.MethodsCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}")
      result.addAllElements(FUNCTIONS.map { create(it) })
      result.addAllElements(GLOBAL_SCOPES.map { createScope(it) })
    }
  }

  private object VariableCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      if (parent !is ILVariable) return
      val pp = parent.parent
      if (pp !is ILSelectExpression) return
      val from = pp.from
      if (from !is ILVariable) return
      if ("var" != from.name) return
      LOG.debug("TIL.VariableCompletionProvider{position=$position, parent=$parent, pp=$pp}")
      val variables: List<Variable> = getLocalDefinedVariables(position);
      for (v in variables) {
        result.addElement(create(v.name))
      }
    }
  }
}

private fun getLocalDefinedVariables(element: PsiElement): List<Variable> {
  val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return emptyList()
  if (host !is HCLElement) return emptyList()
  val module = host.getTerraformModule()
  return module.getAllVariables()
}

