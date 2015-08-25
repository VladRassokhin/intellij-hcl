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
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns.not
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.codeinsight.HCLCompletionProvider
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage
import org.intellij.plugins.hcl.terraform.config.model.Model
import java.util.*

public class TerraformConfigCompletionProvider : HCLCompletionProvider() {
  init {
    val WhiteSpace = psiElement(javaClass<PsiWhiteSpace>())
    val ID = psiElement(HCLElementTypes.ID)

    val Identifier = psiElement(javaClass<HCLIdentifier>())
    val File = psiElement(javaClass<HCLFile>())
    val Block = psiElement(javaClass<HCLBlock>())

    val TerraformConfigFile = psiFile(javaClass<HCLFile>()).withLanguage(TerraformLanguage)

    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(File)
        .andNot(psiElement().afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))),
        BlockKeywordCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier).withSuperParent(2, Block)
        .withParent(not(psiElement(javaClass<HCLIdentifier>()).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))),
        BlockKeywordCompletionProvider);

    // TODO: Provide data from all resources in folder (?)

    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(not(Identifier))
        .andOr(psiElement().withSuperParent(1, File), psiElement().withSuperParent(1, Block))
        .afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))
        , BlockTypeOrNameCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(javaClass<HCLIdentifier>()).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))
        .andOr(psiElement().withSuperParent(2, File), psiElement().withSuperParent(2, Block))
        , BlockTypeOrNameCompletionProvider);


    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(psiFile(javaClass<HCLFile>()).withLanguage(TerraformLanguage))
        .withParent(javaClass<HCLObject>())
        .withSuperParent(2, javaClass<HCLBlock>())
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
        return assert(false)
      }
      result.addAllElements(BLOCK_KEYWORDS.map { LookupElementBuilder.create(it) })
    }
  }

  private object BlockTypeOrNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      LOG.debug("TF.BlockTypeOrNameCompletionProvider")
      val position = parameters.getPosition()
      LOG.debug("position = $position")
      val parent = position.getParent()
      LOG.debug("parent = $parent")
      val obj = when {
        parent is HCLIdentifier -> parent
        position.node.elementType == HCLElementTypes.ID -> position // In case of two IDs (not Identifiers) nearby (start of block in empty file)
        else -> return assert(false)
      }
      LOG.debug("obj = $obj")
      val leftNWS = obj.getPrevSiblingNonWhiteSpace()
      LOG.debug("leftNWS = $leftNWS")
      val type: String = when {
        leftNWS is HCLIdentifier -> leftNWS.id
        leftNWS?.getNode()?.getElementType() == HCLElementTypes.ID -> leftNWS!!.text
        else -> return assert(false)
      }
      when (type) {
        "resource" ->
          result.addAllElements(Model.resources.map { LookupElementBuilder.create(it.type) })

        "provider" ->
          result.addAllElements(Model.providers.map { LookupElementBuilder.create(it.type) })
      }
      return
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
          val tt = when (type) {
            is HCLIdentifier -> type.id
            is HCLStringLiteral -> type.value
            else -> return
          }
          if (tt == "resource") {
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


public fun <T : PsiElement, Self : PsiElementPattern<T, Self>> PsiElementPattern<T, Self>.afterSiblingSkipping2(skip: ElementPattern<out Any>, pattern: ElementPattern<out PsiElement>): Self {
  return with(object : PatternCondition<T>("afterSiblingSkipping2") {
    override fun accepts(t: T, context: ProcessingContext): Boolean {
      var o = t.prevSibling
      while (o != null) {
        if (!skip.accepts(o, context)) {
          return pattern.accepts(o, context)
        }
        o = o.prevSibling
      }
      return false
    }
  })
}