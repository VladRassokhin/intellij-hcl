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
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.StandardPatterns.not
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import getNameElementUnquoted
import getPrevSiblingNonWhiteSpace
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.codeinsight.HCLCompletionContributor
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage
import org.intellij.plugins.hcl.terraform.config.model.*
import org.intellij.plugins.hil.HILFileType
import java.util.*

public class TerraformConfigCompletionContributor : HCLCompletionContributor() {
  init {
    val WhiteSpace = psiElement(PsiWhiteSpace::class.java)
    val ID = psiElement(HCLElementTypes.ID)

    val Identifier = psiElement(HCLIdentifier::class.java)
    val Literal = psiElement(HCLStringLiteral::class.java)
    val File = psiElement(HCLFile::class.java)
    val Block = psiElement(HCLBlock::class.java)
    val Property = psiElement(HCLProperty::class.java)
    val Object = psiElement(HCLObject::class.java)

    val TerraformConfigFile = psiFile(HCLFile::class.java).withLanguage(TerraformLanguage)

    val AtLeastOneEOL = psiElement(PsiWhiteSpace::class.java).withText(StandardPatterns.string().contains("\n"))
    val Nothing = StandardPatterns.alwaysFalse<PsiElement>()

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
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(HCLStringLiteral::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))
        .andOr(psiElement().withSuperParent(2, File), psiElement().withSuperParent(2, Block))
        , BlockTypeOrNameCompletionProvider);
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
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

    // Leftmost identifier of block could be start of new property in case of eol betwen it ant next identifier
    //```
    //resource "X" "Y" {
    //  count<caret>
    //  provider {}
    //}
    //```
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(HCLIdentifier::class.java).beforeLeafSkipping(Nothing, AtLeastOneEOL))
        .withSuperParent(2, Block)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , BlockPropertiesCompletionProvider);

    // Property value
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , PropertyValueCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(Literal)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , PropertyValueCompletionProvider)
  }

  companion object {
    @JvmField public val ROOT_BLOCK_KEYWORDS: SortedSet<String> = TypeModel.RootBlocks.map { it -> it.literal }.toSortedSet()
    public val ROOT_BLOCKS_SORTED: List<PropertyOrBlockType> = TypeModel.RootBlocks.map { it.toPOBT() }.sortedBy { it.name }

    private val LOG = Logger.getInstance(TerraformConfigCompletionContributor::class.java)
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

    fun create(value: PropertyOrBlockType, lookupString: String? = null): LookupElementBuilder {
      var builder = LookupElementBuilder.create(value, lookupString ?: value.name)
      builder = builder.withRenderer(TerraformLookupElementRenderer())
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

  private object PreferRequiredProperty : LookupElementWeigher("hcl.required.property") {
    override fun weigh(element: LookupElement): Comparable<Nothing>? {
      val obj = element.`object`
      if (obj is PropertyOrBlockType) {
        if (obj.required) return 0;
        else return 1
      }
      return 10;
    }
  }

  public abstract class OurCompletionProvider : CompletionProvider<CompletionParameters>() {
    protected fun getTypeModel(): TypeModel {
      val provider = ServiceManager.getService(TypeModelProvider::class.java)
      return provider.get()
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun addResultsWithCustomSorter(result: CompletionResultSet, parameters: CompletionParameters, toAdd: Collection<LookupElementBuilder>) {
      if (toAdd.isEmpty()) return
      result
          .withRelevanceSorter(
              // CompletionSorter.defaultSorter(parameters, result.prefixMatcher)
              CompletionSorter.emptySorter()
                  .weigh(PreferRequiredProperty))
          .addAllElements(toAdd)
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

  object BlockTypeOrNameCompletionProvider : OurCompletionProvider() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val list = SmartList<LookupElementBuilder>()
      doCompletion(position, list)
      result.addAllElements(list)
    }

    public fun doCompletion(position: PsiElement, consumer: MutableList<LookupElementBuilder>) {
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
          consumer.addAll(getTypeModel().resources.map { create(it.type) })

        "provider" ->
          consumer.addAll(getTypeModel().providers.map { create(it.type) })

        "provisioner" ->
          consumer.addAll(getTypeModel().provisioners.map { create(it.type) })
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
                if (injectedPsi.fileType == HILFileType) {
                  right = Types.StringWithInjection;
                }
              }
            })
          }
          isProperty = true
        } else if (pob is HCLBlock) {
          isBlock = true
          if (pob.nameElements.firstOrNull() == _parent) {
            if (_parent.nextSibling is PsiWhiteSpace && _parent.nextSibling.text.contains("\n")) {
              isBlock = false
              _parent = _parent.parent.parent
            }
          }
        }
        if (isBlock || isProperty) {
          _parent = pob?.parent // Object
        }
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
          val props = ModelHelper.getBlockProperties(pp)
          doAddCompletion(isBlock, isProperty, parent, result, right, parameters, props)
        }
      }
    }

    private fun doAddCompletion(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?, parameters: CompletionParameters, properties: Array<out PropertyOrBlockType>) {
      if (properties.isEmpty()) return
      addResultsWithCustomSorter(result, parameters, properties
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

  private object PropertyValueCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      LOG.debug("TF.PropertyValueCompletionProvider{position=$position, parent=$parent}")
      val property = PsiTreeUtil.getParentOfType(position, HCLProperty::class.java) ?: return
      val block = PsiTreeUtil.getParentOfType(property, HCLBlock::class.java) ?: return

      if (property.name == "provider" && block.getNameElementUnquoted(0) == "resource") {
        val providers = property.getTerraformModule().getDefinedProviders()
        result.addAllElements(providers.map { create(it.second) })
      }
      // TODO: Support hints
      //      val props = ModelHelper.getBlockProperties(block).map { it.property }.filterNotNull()
      //      val hints = props.filter { it.name == property.name && it.hint != null }.map { it.hint }.filterIsInstance<String>()
      //      val hint = hints.firstOrNull() ?: return
      //      if (hint.matches("Reference\(.*\)"))
      //
    }

  }
}

object ModelHelper {
  private val LOG = Logger.getInstance(ModelHelper::class.java)

  public fun getBlockProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(0) ?: return emptyArray()
    val props: Array<out PropertyOrBlockType>
    if (type in TypeModel.RootBlocksMap.keys && block.parent !is PsiFile) {
      return emptyArray()
    }
    props = when (type) {
      "provider" -> getProviderProperties(block)
      "resource" -> getResourceProperties(block)

    // Inner for 'resource'
      "lifecycle" -> TypeModel.ResourceLifecycle.properties
      "provisioner" -> getProvisionerProperties(block)
    // Can be inner for both 'resource' and 'provisioner'
      "connection" -> getConnectionProperties(block)
      else -> return TypeModel.RootBlocksMap[type]?.properties?:getModelBlockProperties(block, type)
    }
    return props
  }

  private fun getModelBlockProperties(block: HCLBlock, type: String): Array<out PropertyOrBlockType> {
    // TODO: Speedup, remove recursive up-traverse
    val bp = block.parent
    if (bp is HCLObject) {
      val bpp = bp.parent
      if (bpp is HCLBlock) {
        val properties = getBlockProperties(bpp)
        val candidates = properties.mapNotNull { it.block }.filter { it.literal == type }
        return candidates.map { it.properties.toList() }.flatMap { it }.toTypedArray()
      } else return emptyArray()
    }
    return emptyArray()
  }

  public fun getProviderProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val providerType = if (type != null) getTypeModel().getProviderType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractProvider.properties)
    if (providerType?.properties != null) {
      properties.addAll(providerType?.properties)
    }
    return properties.toTypedArray()
  }

  public fun getProvisionerProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val provisionerType = if (type != null) getTypeModel().getProvisionerType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractResourceProvisioner.properties)
    if (provisionerType?.properties != null) {
      properties.addAll(provisionerType?.properties)
    }
    return properties.toTypedArray()
  }

  public fun getConnectionProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.`object`?.findProperty("type")?.value
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.Connection.properties)
    if (type is HCLStringLiteral) {
      val v = type.value.toLowerCase().trim()
      when (v) {
        "ssh" -> properties.addAll(TypeModel.ConnectionPropertiesSSH)
        "winrm" -> properties.addAll(TypeModel.ConnectionPropertiesWinRM)
      // TODO: Support interpolation resolving
        else -> LOG.warn("Unsupported 'connection' block type '${type.value}'")
      }
    }
    if (type == null) {
      // ssh by default
      properties.addAll(TypeModel.ConnectionPropertiesSSH)
    }
    return properties.toTypedArray()
  }

  public fun getResourceProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val resourceType = if (type != null) getTypeModel().getResourceType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractResource.properties)
    if (resourceType?.properties != null) {
      properties.addAll(resourceType?.properties)
    }
    return ( properties.toTypedArray())
  }


  fun getTypeModel(): TypeModel {
    val provider = ServiceManager.getService(TypeModelProvider::class.java)
    return provider.get()
  }
}
