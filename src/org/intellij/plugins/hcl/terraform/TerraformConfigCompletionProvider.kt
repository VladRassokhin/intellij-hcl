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
import com.intellij.psi.impl.DebugUtil
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.codeinsight.HCLCompletionProvider
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage
import org.intellij.plugins.hcl.terraform.config.model.DefaultResourceTypeProperties
import org.intellij.plugins.hcl.terraform.config.model.Model
import org.intellij.plugins.hcl.terraform.config.model.ModelUtil
import org.intellij.plugins.hcl.terraform.config.model.Type
import java.util.*

public class TerraformConfigCompletionProvider : HCLCompletionProvider() {
  init {
    val WhiteSpace = psiElement(javaClass<PsiWhiteSpace>())
    val ID = psiElement(HCLElementTypes.ID)

    val Identifier = psiElement(javaClass<HCLIdentifier>())
    val File = psiElement(javaClass<HCLFile>())
    val Block = psiElement(javaClass<HCLBlock>())
    val Property = psiElement(javaClass<HCLProperty>())
    val Object = psiElement(javaClass<HCLObject>())

    val TerraformConfigFile = psiFile(javaClass<HCLFile>()).withLanguage(TerraformLanguage)

    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(File)
        .andNot(psiElement().afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))),
        BlockKeywordCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Block)
        .withSuperParent(3, File)
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
        .inFile(TerraformConfigFile)
        .withParent(Object)
        .withSuperParent(2, Block)
        , ResourcePropertiesCompletionProvider);

    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , ResourcePropertiesCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Block)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
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
    public val COMMON_RESOURCE_PROPERTIES: SortedSet<String> = DefaultResourceTypeProperties.map { it.name }.toSortedSet()

    private val LOG = Logger.getInstance(javaClass<TerraformConfigCompletionProvider>())
    fun DumpPsiFileModel(element: PsiElement): () -> String {
      return { DebugUtil.psiToString(element.getContainingFile(), true) }
    }
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
        return assert(false, DumpPsiFileModel(position))
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
        else -> return assert(false, DumpPsiFileModel(position))
      }
      LOG.debug("obj = $obj")
      val leftNWS = obj.getPrevSiblingNonWhiteSpace()
      LOG.debug("leftNWS = $leftNWS")
      val type: String = when {
        leftNWS is HCLIdentifier -> leftNWS.id
        leftNWS?.getNode()?.getElementType() == HCLElementTypes.ID -> leftNWS!!.text
        else -> return assert(false, DumpPsiFileModel(position))
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
      var _parent: PsiElement? = position.parent
      LOG.debug("_parent = $_parent")
      var right: Type? = null;
      var isProperty = false;
      var isBlock = false;
      if (_parent is HCLIdentifier) {
        val pob = _parent.parent // Property or Block
        if (pob is HCLProperty) {
          right = ModelUtil.getValueType(pob.value)
          isProperty = true
        } else if (pob is HCLBlock) {
          isBlock = true
        }
        _parent = pob?.parent // Object
      }
      val parent: PsiElement = _parent ?: return assert(false, DumpPsiFileModel(position));
      assert(parent is HCLObject, DumpPsiFileModel(position))
      if (parent is HCLObject) {
        val pp = parent.getParent()
        if (pp is HCLBlock) {
          val tt = pp.getNameElementUnquoted(0)
          if (tt == "resource") {
            val type = pp.getNameElementUnquoted(1)
            val resourceType = if (type != null) Model.getResourceType(type) else null
            val properties = resourceType?.properties ?: DefaultResourceTypeProperties
            result.addAllElements (properties
                .filter { (isProperty && it.property != null && it.property.type == right) || (isBlock && it.block != null) || (!isProperty && !isBlock) }
                .map { it.name }
                .filter { parent.findProperty(it) == null }
                // TODO: Better renderer for properties/blocks
                .map { LookupElementBuilder.create(it) })
          }
        }
      }
    }
  }
}

fun HCLBlock.getNameElementUnquoted(i: Int): String? {
  val elements = this.nameElements
  if (elements.size() < i - 1) return null
  val element = elements.get(i)
  return when (element) {
    is HCLIdentifier -> element.id
    is HCLStringLiteral -> element.value
    else -> null
  }
}

fun PsiElement.getPrevSiblingNonWhiteSpace(): PsiElement? {
  var prev = this.getPrevSibling()
  while (prev != null && prev is PsiWhiteSpace) {
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