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
package org.intellij.plugins.hcl.terraform.config.codeinsight

import afterSiblingSkipping2
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.patterns.StandardPatterns.not
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext
import getNameElementUnquoted
import getPrevSiblingNonWhiteSpace
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.codeinsight.HCLCompletionProvider
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage
import org.intellij.plugins.hcl.terraform.config.model.*
import org.intellij.plugins.hcl.terraform.il.ILFileType
import java.util.*

public class TerraformConfigCompletionProvider : HCLCompletionProvider() {
  init {
    val WhiteSpace = psiElement(PsiWhiteSpace::class.java)
    val ID = psiElement(HCLElementTypes.ID)

    val Identifier = psiElement(HCLIdentifier::class.java)
    val File = psiElement(HCLFile::class.java)
    val Block = psiElement(HCLBlock::class.java)
    val Property = psiElement(HCLProperty::class.java)
    val Object = psiElement(HCLObject::class.java)

    val TerraformConfigFile = psiFile(HCLFile::class.java).withLanguage(TerraformLanguage)

    val QuotedStringsTokenSet = TokenSet.create(HCLElementTypes.DOUBLE_QUOTED_STRING, HCLElementTypes.SINGLE_QUOTED_STRING)

    // Block first word
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
        .withParent(not(psiElement(HCLIdentifier::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))),
        BlockKeywordCompletionProvider);

    // TODO: Provide data from all resources in folder (?)

    // Block type or name
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(not(Identifier))
        .andOr(psiElement().withSuperParent(1, File), psiElement().withSuperParent(1, Block))
        .afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))
        , BlockTypeOrNameCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(HCLIdentifier::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))
        .andOr(psiElement().withSuperParent(2, File), psiElement().withSuperParent(2, Block))
        , BlockTypeOrNameCompletionProvider);
    extend(CompletionType.BASIC, psiElement().withElementType(QuotedStringsTokenSet)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(HCLStringLiteral::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))
        .andOr(psiElement().withSuperParent(2, File), psiElement().withSuperParent(2, Block))
        , BlockTypeOrNameCompletionProvider);
    extend(CompletionType.BASIC, psiElement().withElementType(QuotedStringsTokenSet)
        .inFile(TerraformConfigFile)
        .andOr(psiElement().withParent(File), psiElement().withParent(Block))
        .afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))
        , BlockTypeOrNameCompletionProvider);

    // Block property
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Object)
        .withSuperParent(2, Block)
        , BlockPropertiesCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , BlockPropertiesCompletionProvider);
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Block)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , BlockPropertiesCompletionProvider);

  }

  companion object {
    public val ROOT_BLOCK_KEYWORDS: SortedSet<String> = TypeModel.RootBlocks.map { it -> it.literal }.toSortedSet()
    public val ROOT_BLOCKS_SORTED: List<PropertyOrBlockType> = TypeModel.RootBlocks.map { it.toPOBT() }.sortedBy { it.name }

    private val LOG = Logger.getInstance(TerraformConfigCompletionProvider::class.java)
    fun DumpPsiFileModel(element: PsiElement): () -> String {
      return { DebugUtil.psiToString(element.containingFile, true) }
    }

    fun create(value: String, quote: Boolean = true): LookupElementBuilder {
      var builder = LookupElementBuilder.create(value)
      if (quote) {
        builder = builder.withInsertHandler(QuoteInsertHandler)
      }
      return builder
    }

    fun create(value: PropertyOrBlockType): LookupElementBuilder {
      // TODO: Better renderer for properties/blocks (#withRenderer)
      var builder = LookupElementBuilder.create(value.name)
      if (value.block != null) {
        builder = builder.withInsertHandler(ResourceBlockNameInsertHandler)
      } else if (value.property != null) {
        builder = builder.withInsertHandler(ResourcePropertyInsertHandler)
      }
      return builder
    }

    private fun failIfInUnitTestsMode(position: PsiElement, addition: String? = null) {
      assert(!ApplicationManager.getApplication().isUnitTestMode, {
        var ret: String = ""
        if (addition != null) {
          ret = "$addition\n"
        }
        ret += " Position: $position\nFile: " + DumpPsiFileModel(position)()
        ret
      })
    }
  }

  private abstract class OurCompletionProvider : CompletionProvider<CompletionParameters>() {
    protected fun getTypeModel(): TypeModel {
      val provider = ServiceManager.getService(TypeModelProvider::class.java)
      return provider.get()
    }
  }

  private object BlockKeywordCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug("TF.BlockKeywordCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}")
      if (leftNWS is HCLIdentifier || leftNWS?.node?.elementType == HCLElementTypes.ID) {
        return assert(false, DumpPsiFileModel(position))
      }
      result.addAllElements(ROOT_BLOCKS_SORTED.map { create(it) })
    }
  }

  private object BlockTypeOrNameCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      LOG.debug("TF.BlockTypeOrNameCompletionProvider{position=$position, parent=$parent}")
      val obj = when {
        parent is HCLIdentifier -> parent
        parent is HCLStringLiteral -> parent
      // Next two cases in case of two IDs (not Identifiers) nearby (start of block in empty file)
        position.node.elementType == HCLElementTypes.ID -> position
        position.node.elementType == HCLElementTypes.STRING_LITERAL -> position
        position.node.elementType == HCLElementTypes.DOUBLE_QUOTED_STRING -> position
        position.node.elementType == HCLElementTypes.SINGLE_QUOTED_STRING -> position
        else -> return failIfInUnitTestsMode(position)
      }
      val leftNWS = obj.getPrevSiblingNonWhiteSpace()
      LOG.debug("TF.BlockTypeOrNameCompletionProvider{position=$position, parent=$parent, obj=$obj, lnws=$leftNWS}")
      val type: String = when {
        leftNWS is HCLIdentifier -> leftNWS.id
        leftNWS?.node?.elementType == HCLElementTypes.ID -> leftNWS!!.text
        else -> return failIfInUnitTestsMode(position)
      }
      when (type) {
        "resource" ->
          result.addAllElements(getTypeModel().resources.map { create(it.type) })

        "provider" ->
          result.addAllElements(getTypeModel().providers.map { create(it.type) })

        "provisioner" ->
          result.addAllElements(getTypeModel().provisioners.map { create(it.type) })
      }
      return
    }
  }

  private object BlockPropertiesCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      var _parent: PsiElement? = position.parent
      var right: Type? = null;
      var isProperty = false;
      var isBlock = false;
      if (_parent is HCLIdentifier) {
        val pob = _parent.parent // Property or Block
        if (pob is HCLProperty) {
          val value = pob.value
          right = ModelUtil.getValueType(value)
          if (right == Types.String && value is PsiLanguageInjectionHost) {
            // Check for Injection
            InjectedLanguageManager.getInstance(pob.project).enumerate(value, object : PsiLanguageInjectionHost.InjectedPsiVisitor {
              override fun visit(injectedPsi: PsiFile, places: MutableList<PsiLanguageInjectionHost.Shred>) {
                if (injectedPsi.fileType == ILFileType) {
                  right = Types.StringWithInjection;
                }
              }
            })
          }
          isProperty = true
        } else if (pob is HCLBlock) {
          isBlock = true
        }
        _parent = pob?.parent // Object
        LOG.debug("TF.BlockPropertiesCompletionProvider{position=$position, parent=$_parent, right=$right, isBlock=$isBlock, isProperty=$isProperty}")
      } else {
        LOG.debug("TF.BlockPropertiesCompletionProvider{position=$position, parent=$_parent, no right part}")
      }
      val parent: PsiElement = _parent ?: return failIfInUnitTestsMode(position);
      if (parent !is HCLObject) {
        return failIfInUnitTestsMode(position, "Parent should be HCLObject")
      }
      assert(parent is HCLObject, DumpPsiFileModel(position))
      if (parent is HCLObject) {
        val pp = parent.parent
        if (pp is HCLBlock) {
          val tt = pp.getNameElementUnquoted(0)
          when (tt) {
            "atlas" -> doAddAtlas(isBlock, isProperty, parent, result, right)
            "module" -> doAddModule(isBlock, isProperty, parent, result, right)
            "output" -> doAddOutput(isBlock, isProperty, parent, result, right)
            "provider" -> doAddProvider(isBlock, isProperty, parent, pp, result, right)
            "resource" -> doAddResource(isBlock, isProperty, parent, pp, result, right)
            "variable" -> doAddVariable(isBlock, isProperty, parent, result, right)

          // Inner for 'resource'
            "lifecycle" -> doAddLifecycle(isBlock, isProperty, parent, result, right)
            "provisioner" -> doAddProvisioner(isBlock, isProperty, parent, pp, result, right)
          // Can be inner for both 'resource' and 'provisioner'
            "connection" -> doAddConnection(isBlock, isProperty, parent, pp, result, right)
          }
        }
      }
    }

    private fun doAddAtlas(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?) {
      doAddCompletion(isBlock, isProperty, parent, result, right, TypeModel.Atlas.properties)
    }

    private fun doAddModule(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?) {
      doAddCompletion(isBlock, isProperty, parent, result, right, TypeModel.Module.properties)
    }

    private fun doAddOutput(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?) {
      doAddCompletion(isBlock, isProperty, parent, result, right, TypeModel.Output.properties)
    }

    private fun doAddProvider(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, pp: HCLBlock, result: CompletionResultSet, right: Type?) {
      val type = pp.getNameElementUnquoted(1)
      val resourceType = if (type != null) getTypeModel().getProviderType(type) else null
      val properties = ArrayList<PropertyOrBlockType>()
      properties.addAll(TypeModel.AbstractResource.properties)
      if (resourceType?.properties != null) {
        properties.addAll(resourceType?.properties)
      }
      doAddCompletion(isBlock, isProperty, parent, result, right, properties.toTypedArray())
    }

    private fun doAddVariable(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?) {
      doAddCompletion(isBlock, isProperty, parent, result, right, TypeModel.Variable.properties)
    }

    private fun doAddLifecycle(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?) {
      doAddCompletion(isBlock, isProperty, parent, result, right, TypeModel.ResourceLifecycle.properties)
    }

    private fun doAddProvisioner(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, pp: HCLBlock, result: CompletionResultSet, right: Type?) {
      val type = pp.getNameElementUnquoted(1)
      val resourceType = if (type != null) getTypeModel().getProvisionerType(type) else null
      val properties = resourceType?.properties ?: return
      doAddCompletion(isBlock, isProperty, parent, result, right, properties)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun doAddConnection(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, pp: HCLBlock, result: CompletionResultSet, right: Type?) {
      // TODO: Implement
      // doAddCompletion(isBlock, isProperty, parent, result, right, TypeModel.ResourceConnection.properties)
    }

    //    private fun doAdd(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, pp: HCLBlock, result: CompletionResultSet, right: Type?) {
    //      doAddCompletion(isBlock, isProperty, parent, result, right, properties)
    //    }

    private fun doAddResource(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, pp: HCLBlock, result: CompletionResultSet, right: Type?) {
      val type = pp.getNameElementUnquoted(1)
      val resourceType = if (type != null) getTypeModel().getResourceType(type) else null
      val properties = ArrayList<PropertyOrBlockType>()
      properties.addAll(TypeModel.AbstractResource.properties)
      if (resourceType?.properties != null) {
        properties.addAll(resourceType?.properties)
      }
      doAddCompletion(isBlock, isProperty, parent, result, right, properties.toTypedArray())
    }

    private fun doAddCompletion(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?, properties: Array<out PropertyOrBlockType>) {
      result.addAllElements (properties
          .filter { isRightOfPropertyWithCompatibleType(isProperty, it, right) || (isBlock && it.block != null) || (!isProperty && !isBlock) }
          // TODO: Filter should be based on 'max-count' model property (?)
          .filter { (it.property != null && parent.findProperty(it.name) == null) || (it.block != null) }
          .map { create(it) })
    }

    private fun isRightOfPropertyWithCompatibleType(isProperty: Boolean, it: PropertyOrBlockType, right: Type?): Boolean {
      if (!isProperty) return false
      if (it.property == null) return false
      if (right == Types.StringWithInjection) {
        // StringWithInjection may be anything
        // TODO: Check interpolation result
        return true;
      }
      return it.property.type == right
    }
  }
}
