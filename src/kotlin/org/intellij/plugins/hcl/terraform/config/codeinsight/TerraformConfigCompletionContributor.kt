/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import org.intellij.plugins.debug
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.codeinsight.HCLCompletionContributor
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.model.*
import org.intellij.plugins.hcl.terraform.config.psi.TerraformReferenceContributor
import org.intellij.plugins.hil.HILFileType
import org.intellij.plugins.hil.codeinsight.ReferenceCompletionHelper.findByFQNRef
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.TypeCachedValueProvider
import java.util.*

class TerraformConfigCompletionContributor : HCLCompletionContributor() {
  init {
    val WhiteSpace = psiElement(PsiWhiteSpace::class.java)
    val ID = psiElement(HCLElementTypes.ID)

    val Identifier = psiElement(HCLIdentifier::class.java)
    val Literal = psiElement(HCLStringLiteral::class.java)
    val File = psiElement(HCLFile::class.java)
    val Block = psiElement(HCLBlock::class.java)
    val Property = psiElement(HCLProperty::class.java)
    val Object = psiElement(HCLObject::class.java)
    val Array = psiElement(HCLArray::class.java)

    val TerraformConfigFile = TerraformReferenceContributor.TerraformConfigFile

    val AtLeastOneEOL = psiElement(PsiWhiteSpace::class.java).withText(StandardPatterns.string().contains("\n"))
    val Nothing = StandardPatterns.alwaysFalse<PsiElement>()

    // Block first word
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(File)
        .andNot(psiElement().afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))),
        BlockKeywordCompletionProvider)
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Block)
        .withSuperParent(3, File)
        .withParent(not(psiElement(HCLIdentifier::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))),
        BlockKeywordCompletionProvider)

    // TODO: Provide data from all resources in folder (?)

    // Block type or name
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(not(Identifier))
        .andOr(psiElement().withSuperParent(1, File), psiElement().withSuperParent(1, Block))
        .afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))
        , BlockTypeOrNameCompletionProvider)
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(HCLIdentifier::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))
        .andOr(psiElement().withSuperParent(2, File), psiElement().withSuperParent(2, Block))
        , BlockTypeOrNameCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(psiElement(HCLStringLiteral::class.java).afterSiblingSkipping2(WhiteSpace, or(ID, Identifier)))
        .andOr(psiElement().withSuperParent(2, File), psiElement().withSuperParent(2, Block))
        , BlockTypeOrNameCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformConfigFile)
        .andOr(psiElement().withParent(File), psiElement().withParent(Block))
        .afterSiblingSkipping2(WhiteSpace, or(ID, Identifier))
        , BlockTypeOrNameCompletionProvider)

    // Block property
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Object)
        .withSuperParent(2, Block)
        , BlockPropertiesCompletionProvider)
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , BlockPropertiesCompletionProvider)
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Block)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , BlockPropertiesCompletionProvider)

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
        , BlockPropertiesCompletionProvider)

    // Property value
    extend(null, psiElement(HCLElementTypes.ID)
        .inFile(TerraformConfigFile)
        .withParent(Identifier)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , PropertyValueCompletionProvider)
    extend(null, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(Literal)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , PropertyValueCompletionProvider)
    // depends_on completion
    extend(null, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(Literal)
        .withSuperParent(2, Array)
        .withSuperParent(3, Property)
        .withSuperParent(4, Object)
        .withSuperParent(5, Block)
        , PropertyValueCompletionProvider)


    // Variables in .tvars files
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformReferenceContributor.TerraformVariablesFile)
        .andOr(
            psiElement()
                .withParent(File),
            psiElement()
                .withParent(Identifier)
                .withSuperParent(2, Property)
                .withSuperParent(3, File)
        ), VariableNameTFVARSCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformReferenceContributor.TerraformVariablesFile)
        .andOr(
            psiElement()
                .withParent(File),
            psiElement()
                .withParent(Literal)
                .withSuperParent(2, Property)
                .withSuperParent(3, File)
        ), VariableNameTFVARSCompletionProvider)
    extend(CompletionType.BASIC, psiElement(HCLElementTypes.ID)
        .inFile(TerraformReferenceContributor.TerraformVariablesFile)
        .andOr(
            psiElement()
                .withSuperParent(1, Identifier)
                .withSuperParent(2, Property)
                .withSuperParent(3, Object)
                .withSuperParent(4, Property)
                .withSuperParent(5, File),
            psiElement()
                .withSuperParent(1, Object)
                .withSuperParent(2, Property)
                .withSuperParent(3, File)
        ), MappedVariableTFVARSCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.STRING_LITERALS)
        .inFile(TerraformReferenceContributor.TerraformVariablesFile)
        .andOr(
            psiElement()
                .withSuperParent(1, Literal)
                .withSuperParent(2, Property)
                .withSuperParent(3, Object)
                .withSuperParent(4, Property)
                .withSuperParent(5, File),
            psiElement()
                .withSuperParent(1, Object)
                .withSuperParent(2, Property)
                .withSuperParent(3, File)
        ), MappedVariableTFVARSCompletionProvider)
  }

  companion object {
    @JvmField val ROOT_BLOCK_KEYWORDS: SortedSet<String> = TypeModel.RootBlocks.map(BlockType::literal).toSortedSet()
    val ROOT_BLOCKS_SORTED: List<PropertyOrBlockType> = TypeModel.RootBlocks.map { it.toPOBT() }.sortedBy { it.name }

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
      LOG.assertTrue(!ApplicationManager.getApplication().isUnitTestMode, {
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
        if (obj.required) return 0
        else return 1
      }
      return 10
    }
  }

  abstract class OurCompletionProvider : CompletionProvider<CompletionParameters>() {
    protected fun getTypeModel(project: Project): TypeModel {
      return TypeModelProvider.getModel(project)
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
      LOG.debug { "TF.BlockKeywordCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}" }
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
      doCompletion(position, list, parameters.invocationCount)
      result.addAllElements(list)
    }

    fun doCompletion(position: PsiElement, consumer: MutableList<LookupElementBuilder>, invocationCount: Int = 1) {
      val parent = position.parent
      LOG.debug { "TF.BlockTypeOrNameCompletionProvider{position=$position, parent=$parent}" }
      val obj = when {
        parent is HCLIdentifier -> parent
        parent is HCLStringLiteral -> parent
      // Next line for the case of two IDs (not Identifiers) nearby (start of block in empty file) TODO: check that
        HCLParserDefinition.IDENTIFYING_LITERALS.contains(position.node.elementType) -> position
        else -> return failIfInUnitTestsMode(position)
      }
      val leftNWS = obj.getPrevSiblingNonWhiteSpace()
      LOG.debug { "TF.BlockTypeOrNameCompletionProvider{position=$position, parent=$parent, obj=$obj, lnws=$leftNWS}" }
      val type: String = when {
        leftNWS is HCLIdentifier -> leftNWS.id
        leftNWS?.node?.elementType == HCLElementTypes.ID -> leftNWS!!.text
        else -> return failIfInUnitTestsMode(position)
      }
      val cache = HashMap<String, Boolean>()
      val project = position.project
      when (type) {
        "resource" ->
          consumer.addAll(getTypeModel(project).resources.filter { invocationCount >= 3 || isProviderUsed(parent, it.provider.type, cache) }.map { create(it.type) })

        "data" ->
          consumer.addAll(getTypeModel(project).dataSources.filter { invocationCount >= 3 || isProviderUsed(parent, it.provider.type, cache) }.map { create(it.type) })

        "provider" ->
          consumer.addAll(getTypeModel(project).providers.map { create(it.type) })

        "provisioner" ->
          consumer.addAll(getTypeModel(project).provisioners.map { create(it.type) })
      }
      return
    }

    fun isProviderUsed(element: PsiElement, providerName: String, cache: MutableMap<String, Boolean>): Boolean {
      val hclElement = PsiTreeUtil.getParentOfType(element, HCLElement::class.java, false)
      if (hclElement == null) {
        failIfInUnitTestsMode(element, "Completion called on element without any HCLElement as parent")
        return true
      }
      return isProviderUsed(hclElement.getTerraformModule(), providerName, cache)

    }

    fun isProviderUsed(module: Module, providerName: String, cache: MutableMap<String, Boolean>): Boolean {
      if (!cache.containsKey(providerName)) {
        val providers = module.getDefinedProviders()
        cache[providerName] = providers.isEmpty() || providers.any { it.first.name == providerName }
            || module.model.getProviderType(providerName)?.properties?.isEmpty() ?: false
      }
      return cache[providerName]!!
    }
  }

  private object BlockPropertiesCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      var _parent: PsiElement? = position.parent
      var right: Type? = null
      var isProperty = false
      var isBlock = false
      if (_parent is HCLIdentifier) {
        val pob = _parent.parent // Property or Block
        if (pob is HCLProperty) {
          val value = pob.value
          right = value.getValueType()
          if (right == Types.String && value is PsiLanguageInjectionHost) {
            // Check for Injection
            InjectedLanguageManager.getInstance(pob.project).enumerate(value, object : PsiLanguageInjectionHost.InjectedPsiVisitor {
              override fun visit(injectedPsi: PsiFile, places: MutableList<PsiLanguageInjectionHost.Shred>) {
                if (injectedPsi.fileType == HILFileType) {
                  right = Types.StringWithInjection
                  val root = injectedPsi.firstChild
                  if (root == injectedPsi.lastChild && root is ILExpression) {
                    val type = TypeCachedValueProvider.getType(root)
                    if (type != null && type != Types.Any) {
                      right = type
                    }
                  }
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
        LOG.debug { "TF.BlockPropertiesCompletionProvider{position=$position, parent=$_parent, right=$right, isBlock=$isBlock, isProperty=$isProperty}" }
      } else {
        LOG.debug { "TF.BlockPropertiesCompletionProvider{position=$position, parent=$_parent, no right part}" }
      }
      val parent: HCLObject = _parent as? HCLObject ?: return failIfInUnitTestsMode(position, "Parent should be HCLObject")
      val pp = parent.parent
      if (pp is HCLBlock) {
        val props = ModelHelper.getBlockProperties(pp)
        doAddCompletion(isBlock, isProperty, parent, result, right, parameters, props)
      }
    }

    private fun doAddCompletion(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?, parameters: CompletionParameters, properties: Array<out PropertyOrBlockType>) {
      if (properties.isEmpty()) return
      addResultsWithCustomSorter(result, parameters, properties
          .filter { it.name != "__has_dynamic_attributes" }
          .filter { isRightOfPropertyWithCompatibleType(isProperty, it, right) || (isBlock && it.block != null) || (!isProperty && !isBlock) }
          // TODO: Filter should be based on 'max-count' model property (?)
          .filter { (it.property != null && parent.findProperty(it.name) == null) || (it.block != null) }
          .map { create(it) })
    }

    private fun isRightOfPropertyWithCompatibleType(isProperty: Boolean, it: PropertyOrBlockType, right: Type?): Boolean {
      if (!isProperty) return false
      if (it.property == null) return false
      if (right == Types.StringWithInjection) {
        // StringWithInjection means TypeCachedValueProvider was unable to understand type of interpolation
        return true
      }
      return it.property.type == right
    }
  }

  private object PropertyValueCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      val inArray = (parent.parent is HCLArray)
      LOG.debug { "TF.PropertyValueCompletionProvider{position=$position, parent=$parent}" }
      val property = PsiTreeUtil.getParentOfType(position, HCLProperty::class.java) ?: return
      val block = PsiTreeUtil.getParentOfType(property, HCLBlock::class.java) ?: return

      val type = block.getNameElementUnquoted(0)
      // TODO: Replace with 'ReferenceHint'
      if (property.name == "provider" && (type == "resource" || type == "data")) {
        val providers = property.getTerraformModule().getDefinedProviders()
        result.addAllElements(providers.map { create(it.second) })
        return
      }
      if (property.name == "depends_on" && (type == "resource" || type == "data") && inArray) {
        val resources = property.getTerraformModule().getDeclaredResources()
            .map { "${it.getNameElementUnquoted(1)}.${it.name}" }
        val datas = property.getTerraformModule().getDeclaredDataSources()
            .map { "data.${it.getNameElementUnquoted(1)}.${it.name}" }

        val current = (if (type == "data") "data." else "") + "${block.getNameElementUnquoted(1)}.${block.name}"

        result.addAllElements(resources.plus(datas).minus(current).map { create(it) })
        return
      }
      val props = ModelHelper.getBlockProperties(block).map { it.property }.filterNotNull()
      val hints = props.filter { it.name == property.name && it.hint != null }.map { it.hint }
      val hint = hints.firstOrNull() ?: return
      if (hint is SimpleValueHint) {
        result.addAllElements(hint.hint.map { create(it) })
        return
      }
      if (hint is ReferenceHint) {
        val module = property.getTerraformModule()
        hint.hint
            .mapNotNull { findByFQNRef(it, module) }
            .flatMap { it }
            .mapNotNull { it ->
              return@mapNotNull when (it) {
                // TODO: Enable or remove next two lines
//                is HCLBlock -> HCLQualifiedNameProvider.getQualifiedModelName(it)
//                is HCLProperty -> HCLQualifiedNameProvider.getQualifiedModelName(it)
                is String -> "" + '$' + "{$it}"
                else -> null
              }
            }
            .forEach { result.addElement(create(it)) }
        return
      }
      // TODO: Support other hint types
    }

  }

  private object VariableNameTFVARSCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      LOG.debug { "TF.VariableNameTFVARSCompletionProvider{position=$position, parent=$parent}" }
      val module: Module
      if (parent is HCLFile) {
        module = parent.getTerraformModule()
      } else if (parent is HCLElement) {
        val pp = parent.parent as? HCLProperty ?: return
        if (parent !== pp.nameIdentifier) return
        module = parent.getTerraformModule()
      } else return
      val variables = module.getAllVariables()
      result.addAllElements(variables.map { create(it.second.name, false).withInsertHandler(ResourcePropertyInsertHandler) })
    }
  }

  private object MappedVariableTFVARSCompletionProvider : OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      LOG.debug { "TF.MappedVariableTFVARSCompletionProvider{position=$position, parent=$parent}" }
      val varProperty: HCLProperty
      if (parent is HCLObject) {
        val pp = parent.parent
        if (pp is HCLProperty) {
          varProperty = pp
        } else return
      } else if (parent is HCLElement) {
        if (!HCLPsiUtil.isPropertyKey(parent)) return
        val ppp = parent.parent.parent as? HCLObject ?: return
        val pppp = ppp.parent as? HCLProperty ?: return
        varProperty = pppp
      } else return

      if (varProperty.parent !is HCLFile) return

      val variable = varProperty.getTerraformModule().findVariable(varProperty.name) ?: return
      val default = variable.second.`object`?.findProperty("default")?.value as? HCLObject ?: return

      result.addAllElements(default.propertyList.map { create(it.name).withInsertHandler(ResourcePropertyInsertHandler) })
    }
  }
}

object ModelHelper {
  private val LOG = Logger.getInstance(ModelHelper::class.java)

  fun getBlockProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(0) ?: return emptyArray()
    val props: Array<out PropertyOrBlockType>
    if (type in TypeModel.RootBlocksMap.keys && block.parent !is PsiFile) {
      return emptyArray()
    }
    props = when (type) {
      "provider" -> getProviderProperties(block)
      "resource" -> getResourceProperties(block)
      "data" -> getDataSourceProperties(block)

    // Inner for 'resource'
      "lifecycle" -> TypeModel.ResourceLifecycle.properties
      "provisioner" -> getProvisionerProperties(block)
    // Can be inner for both 'resource' and 'provisioner'
      "connection" -> getConnectionProperties(block)

      "module" -> getModuleProperties(block)
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

  fun getProviderProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val providerType = if (type != null) getTypeModel(block.project).getProviderType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractProvider.properties)
    if (providerType?.properties != null) {
      properties.addAll(providerType.properties)
    }
    return properties.toTypedArray()
  }

  fun getProvisionerProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val provisionerType = if (type != null) getTypeModel(block.project).getProvisionerType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractResourceProvisioner.properties)
    if (provisionerType?.properties != null) {
      properties.addAll(provisionerType.properties)
    }
    return properties.toTypedArray()
  }

  fun getConnectionProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
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

  fun getResourceProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val resourceType = if (type != null) getTypeModel(block.project).getResourceType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractResource.properties)
    if (resourceType?.properties != null) {
      properties.addAll(resourceType.properties)
    }
    return ( properties.toTypedArray())
  }

  fun getDataSourceProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val dataSourceType = if (type != null) getTypeModel(block.project).getDataSourceType(type) else null
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.AbstractDataSource.properties)
    if (dataSourceType?.properties != null) {
      properties.addAll(dataSourceType.properties)
    }
    return (properties.toTypedArray())
  }

  fun getModuleProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val properties = ArrayList<PropertyOrBlockType>()
    properties.addAll(TypeModel.Module.properties)

    val module = Module.getAsModuleBlock(block)
    if (module != null) {
      val variables = module.getAllVariables()
      for (v in variables) {
        val name = v.first.name
        val hasDefault = v.second.`object`?.findProperty(TypeModel.Variable_Default.name) != null
        // TODO: Add 'string' hint, AFAIK only strings coud be passed to module parameters
        properties.add(PropertyType(name, Types.String, required = !hasDefault).toPOBT())
      }
    }
    return (properties.toTypedArray())
  }


  fun getTypeModel(project: Project): TypeModel {
    return TypeModelProvider.getModel(project)
  }
}
