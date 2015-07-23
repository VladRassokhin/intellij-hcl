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
package org.intellij.plugins.hcl.codeinsight

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.codeinsight.JsonCompletionContributor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.HCLArray
import org.intellij.plugins.hcl.psi.HCLProperty

/**
 * Based on com.intellij.json.codeinsight.JsonCompletionContributor
 */
public open class HCLCompletionProvider : CompletionContributor() {
  private val LOG = Logger.getInstance(javaClass<JsonCompletionContributor>())

  private val AFTER_EQUALS_IN_PROPERTY = psiElement().afterLeaf("=").withSuperParent(2, javaClass<HCLProperty>())
  private val AFTER_COMMA_OR_BRACKET_IN_ARRAY = psiElement().afterLeaf(",", "[").withSuperParent(2, javaClass<HCLArray>())

  init {
    extend(CompletionType.BASIC, AFTER_EQUALS_IN_PROPERTY, MyKeywordsCompletionProvider)
    // FIXME: Make it work
    extend(CompletionType.BASIC, AFTER_COMMA_OR_BRACKET_IN_ARRAY, MyKeywordsCompletionProvider)
  }

  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    LOG.debug(DebugUtil.psiToString(parameters.getPosition().getContainingFile(), true))
    super.fillCompletionVariants(parameters, result)
  }

  private object MyKeywordsCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val KEYWORDS = arrayOf("null", "true", "false")

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      for (keyword in KEYWORDS) {
        result.addElement(LookupElementBuilder.create(keyword).bold())
      }
    }
  }
}
