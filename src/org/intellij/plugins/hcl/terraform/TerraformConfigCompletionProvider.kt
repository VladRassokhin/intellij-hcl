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
package org.intellij.plugins.hcl.terraform

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.HCLLanguage
import org.intellij.plugins.hcl.codeinsight.HCLCompletionProvider
import org.intellij.plugins.hcl.psi.*
import java.util.TreeSet

public class TerraformConfigCompletionProvider : HCLCompletionProvider() {
  init {
    // TODO: Narrow down pattern to start of property(due to grammar)/block
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HCLLanguage)
        .inVirtualFile(PlatformPatterns.virtualFile().withExtension("tf"))
//        .withParents(javaClass<HCLProperty>(), javaClass<HCLFile>())
        , BlockKeywordCompletionProvider);

    // TODO: Provide data from all resources in folder (?)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HCLLanguage)
        .inVirtualFile(PlatformPatterns.virtualFile().withExtension("tf"))
        //        .withParent(javaClass<HCLObject>())
        //        .withSuperParent(2, javaClass<HCLBlock>())
        , ResourcePropertiesCompletionProvider);
  }

  companion object {
    public val BLOCK_KEYWORDS: TreeSet<String> = sortedSetOf(
        "atlas",
        "module",
        "output",
        "provider",
        "resource",
        "variable"
    )
    public val COMMON_RESOURCE_PARAMETERS: TreeSet<String> = sortedSetOf(
        "id",
        "count"
    )
    private val LOG = Logger.getInstance(javaClass<TerraformConfigCompletionProvider>())
  }

  private object BlockKeywordCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      LOG.debug("TF.BlockKeywordCompletionProvider")
      val position = parameters.getPosition()
      LOG.debug("position = $position")
      val parent = position.getParent()
      LOG.debug("parent = $parent")
      LOG.debug("left = ${position.getPrevSibling()}")
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug("leftNWS = $leftNWS")
      if (leftNWS is HCLIdentifier || leftNWS?.getNode()?.getElementType() == HCLElementTypes.ID) {
        return
      }
      result.addAllElements(BLOCK_KEYWORDS.map { LookupElementBuilder.create(it) })
    }
  }

  private object ResourcePropertiesCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      LOG.debug("TF.ResourcePropertiesCompletionProvider")
      val position = parameters.getPosition()
      LOG.debug("position = $position")
      val parent = position.getParent()
      LOG.debug("parent = $parent")
      LOG.debug("left = ${position.getPrevSibling()}")
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug("leftNWS = $leftNWS")
      if (parent is HCLObject) {
        val pp = parent.getParent()
        if (pp is HCLBlock) {
          val type = pp.getNameElements().iterator().next()
          if (type.getName() == "resource") {
            result.addAllElements(COMMON_RESOURCE_PARAMETERS.filter { parent.findProperty(it) == null }.map { LookupElementBuilder.create(it) })
          }
        }
      }
    }
  }
}

fun PsiElement.getPrevSiblingNonWhiteSpace(): PsiElement? {
  var prev = this.getPrevSibling()
  while(prev != null && prev is PsiWhiteSpace) {
    prev = prev.getPrevSibling()
  }
  return prev;
}
