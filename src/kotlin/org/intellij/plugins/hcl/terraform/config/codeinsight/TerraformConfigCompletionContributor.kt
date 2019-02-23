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
import com.intellij.patterns.PlatformPatterns.not
import com.intellij.patterns.PlatformPatterns.psiElement
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
import org.intellij.plugins.hcl.patterns.HCLPatterns.Array
import org.intellij.plugins.hcl.patterns.HCLPatterns.AtLeastOneEOL
import org.intellij.plugins.hcl.patterns.HCLPatterns.Block
import org.intellij.plugins.hcl.patterns.HCLPatterns.File
import org.intellij.plugins.hcl.patterns.HCLPatterns.FileOrBlock
import org.intellij.plugins.hcl.patterns.HCLPatterns.IdentifierOrStringLiteral
import org.intellij.plugins.hcl.patterns.HCLPatterns.IdentifierOrStringLiteralOrSimple
import org.intellij.plugins.hcl.patterns.HCLPatterns.Nothing
import org.intellij.plugins.hcl.patterns.HCLPatterns.Object
import org.intellij.plugins.hcl.patterns.HCLPatterns.Property
import org.intellij.plugins.hcl.patterns.HCLPatterns.PropertyOrBlock
import org.intellij.plugins.hcl.patterns.HCLPatterns.WhiteSpace
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.Constants
import org.intellij.plugins.hcl.terraform.config.model.*
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.TerraformConfigFile
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.TerraformVariablesFile
import org.intellij.plugins.hil.HILFileType
import org.intellij.plugins.hil.codeinsight.ReferenceCompletionHelper.findByFQNRef
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.TypeCachedValueProvider
import org.intellij.plugins.nullize
import java.util.*

class TerraformConfigCompletionContributor : HCLCompletionContributor() {
  init {

    // Block first word
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(File)
        .andNot(psiElement().afterSiblingSkipping2(WhiteSpace, IdentifierOrStringLiteralOrSimple)),
        BlockKeywordCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(IdentifierOrStringLiteral)
        .withSuperParent(2, Block)
        .withSuperParent(3, File)
        .withParent(not(psiElement().and(IdentifierOrStringLiteral).afterSiblingSkipping2(WhiteSpace, IdentifierOrStringLiteralOrSimple))),
        BlockKeywordCompletionProvider)

    // Block type or name
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(FileOrBlock)
        .afterSiblingSkipping2(WhiteSpace, IdentifierOrStringLiteralOrSimple)
        , BlockTypeOrNameCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(psiElement().and(IdentifierOrStringLiteral).afterSiblingSkipping2(WhiteSpace, IdentifierOrStringLiteralOrSimple))
        .withSuperParent(2, FileOrBlock)
        , BlockTypeOrNameCompletionProvider)

    //region InBlock Property key
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(Object)
        .withSuperParent(2, Block)
        , BlockPropertiesCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(IdentifierOrStringLiteral)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , BlockPropertiesCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(IdentifierOrStringLiteral)
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
    //endregion

    //region InBlock Property value
    extend(null, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(IdentifierOrStringLiteral)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , PropertyValueCompletionProvider)
    // depends_on completion
    extend(null, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(IdentifierOrStringLiteral)
        .withSuperParent(2, Array)
        .withSuperParent(3, Property)
        .withSuperParent(4, Object)
        .withSuperParent(5, Block)
        , PropertyValueCompletionProvider)
    //endregion

    //region InBlock PropertyWithObjectValue Key
    // property = { <caret> }
    // property = { "<caret>" }
    // property { <caret> }
    // property { "<caret>" }
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(Object)
        .withSuperParent(2, PropertyOrBlock)
        .withSuperParent(3, Object)
        .withSuperParent(4, Block)
        , PropertyObjectKeyCompletionProvider)
    // property = { <caret>a="" }
    // property = { "<caret>a"="" }
    // property { <caret>="" }
    // property { "<caret>"="" }
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformConfigFile)
        .withParent(IdentifierOrStringLiteral)
        .withSuperParent(2, Property)
        .withSuperParent(3, Object)
        .withSuperParent(4, PropertyOrBlock)
        .withSuperParent(5, Object)
        .withSuperParent(6, Block)
        , PropertyObjectKeyCompletionProvider)
    //endregion

    //region .tfvars
    // Variables in .tvars files
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformVariablesFile)
        .andOr(
            psiElement()
                .withParent(File),
            psiElement()
                .withParent(IdentifierOrStringLiteral)
                .withSuperParent(2, Property)
                .withSuperParent(3, File)
        ), VariableNameTFVARSCompletionProvider)
    extend(CompletionType.BASIC, psiElement().withElementType(HCLParserDefinition.IDENTIFYING_LITERALS)
        .inFile(TerraformVariablesFile)
        .andOr(
            psiElement()
                .withSuperParent(1, IdentifierOrStringLiteral)
                .withSuperParent(2, Property)
                .withSuperParent(3, Object)
                .withSuperParent(4, Property)
                .withSuperParent(5, File),
            psiElement()
                .withSuperParent(1, Object)
                .withSuperParent(2, Property)
                .withSuperParent(3, File)
        ), MappedVariableTFVARSCompletionProvider)
    //endregion
  }

  companion object {
    @JvmField val ROOT_BLOCK_KEYWORDS: Set<String> = TypeModel.RootBlocks.map(BlockType::literal).toHashSet()
    val ROOT_BLOCKS_SORTED: List<BlockType> = TypeModel.RootBlocks.sortedBy { it.literal }

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
      if (value is BlockType) {
        builder = builder.withInsertHandler(ResourceBlockNameInsertHandler(value))
      } else if (value is PropertyType) {
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

    fun getOriginalObject(parameters: CompletionParameters, obj: HCLObject): HCLObject {
      val originalObject = parameters.originalFile.findElementAt(obj.textRange.startOffset)?.parent
      return originalObject as? HCLObject ?: obj
    }

    fun getClearTextValue(element: PsiElement?): String? {
      return when {
        element == null -> null
        element is HCLIdentifier -> element.id
        element is HCLStringLiteral -> element.value
        element.node?.elementType == HCLElementTypes.ID -> element.text
        HCLParserDefinition.STRING_LITERALS.contains(element.node?.elementType) -> HCLPsiUtil.stripQuotes(element.text)
        else -> return null
      }
    }

    fun getIncomplete(parameters: CompletionParameters): String? {
      val position = parameters.position
      val text = TerraformConfigCompletionContributor.getClearTextValue(position) ?: position.text
      if (text == CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED) return null
      return text.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "").nullize(true)
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
      assert(getClearTextValue(leftNWS) == null, DumpPsiFileModel(position))
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
        // Next line for the case of two IDs (not Identifiers) nearby (start of block in empty file)
        HCLParserDefinition.IDENTIFYING_LITERALS.contains(position.node.elementType) -> position
        else -> return failIfInUnitTestsMode(position)
      }
      val leftNWS = obj.getPrevSiblingNonWhiteSpace()
      LOG.debug { "TF.BlockTypeOrNameCompletionProvider{position=$position, parent=$parent, obj=$obj, lnws=$leftNWS}" }
      val type = getClearTextValue(leftNWS) ?: return failIfInUnitTestsMode(position)
      val cache = HashMap<String, Boolean>()
      val project = position.project
      when (type) {
        "resource" ->
          consumer.addAll(getTypeModel(project).resources.values.filter { invocationCount >= 3 || isProviderUsed(parent, it.provider.type, cache) }.map { create(it.type).withInsertHandler(ResourceBlockSubNameInsertHandler(it)) })

        "data" ->
          consumer.addAll(getTypeModel(project).dataSources.values.filter { invocationCount >= 3 || isProviderUsed(parent, it.provider.type, cache) }.map { create(it.type).withInsertHandler(ResourceBlockSubNameInsertHandler(it)) })

        "provider" ->
          consumer.addAll(getTypeModel(project).providers.values.map { create(it.type).withInsertHandler(ResourceBlockSubNameInsertHandler(it)) })

        "provisioner" ->
          consumer.addAll(getTypeModel(project).provisioners.values.map { create(it.type).withInsertHandler(ResourceBlockSubNameInsertHandler(it)) })

        "backend" ->
          consumer.addAll(getTypeModel(project).backends.values.map { create(it.type).withInsertHandler(ResourceBlockSubNameInsertHandler(it)) })
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
      val original = parameters.originalPosition ?: return
      val original_parent = original.parent
      if (HCLElementTypes.L_CURLY === original.node.elementType && original_parent is HCLObject) {
        LOG.debug { "Origin is '{' inside Object, O.P.P = ${original_parent.parent}" }
        if (original_parent.parent is HCLBlock) return
      }
      if (_parent is HCLIdentifier || _parent is HCLStringLiteral) {
        val pob = _parent.parent // Property or Block
        if (pob is HCLProperty) {
          val value = pob.value
          if (value === _parent) return
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
        LOG.debug { "TF.BlockPropertiesCompletionProvider{position=$position, parent=$_parent, original=$original, right=$right, isBlock=$isBlock, isProperty=$isProperty}" }
      } else {
        LOG.debug { "TF.BlockPropertiesCompletionProvider{position=$position, parent=$_parent, original=$original, no right part}" }
      }
      val parent: HCLObject = _parent as? HCLObject ?: return failIfInUnitTestsMode(position, "Parent should be HCLObject")
      val use = getOriginalObject(parameters, parent)
      val block = use.parent
      if (block is HCLBlock) {
        val props = ModelHelper.getBlockProperties(block)
        doAddCompletion(isBlock, isProperty, use, result, right, parameters, props)
      }
    }

    private fun doAddCompletion(isBlock: Boolean, isProperty: Boolean, parent: HCLObject, result: CompletionResultSet, right: Type?, parameters: CompletionParameters, properties: Array<out PropertyOrBlockType>) {
      if (properties.isEmpty()) return
      val incomplete = getIncomplete(parameters)
      if (incomplete != null) {
        LOG.debug { "Including properties which contains incomplete result: $incomplete" }
      }
      addResultsWithCustomSorter(result, parameters, properties
          .filter { it.name != Constants.HAS_DYNAMIC_ATTRIBUTES }
          .filter { isRightOfPropertyWithCompatibleType(isProperty, it, right) || (isBlock && it is BlockType) || (!isProperty && !isBlock) }
          // TODO: Filter should be based on 'max-count' model property (?)
          .filter { (it is PropertyType && (parent.findProperty(it.name) == null || (incomplete != null && it.name.contains(incomplete)))) || (it is BlockType) }
          .map { create(it) })
    }

    private fun isRightOfPropertyWithCompatibleType(isProperty: Boolean, it: PropertyOrBlockType, right: Type?): Boolean {
      if (!isProperty) return false
      if (it !is PropertyType) return false
      if (right == Types.StringWithInjection) {
        // StringWithInjection means TypeCachedValueProvider was unable to understand type of interpolation
        return true
      }
      return it.type == right
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
      val props = ModelHelper.getBlockProperties(block).filterIsInstance(PropertyType::class.java)
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
    // Special case for 'backend' blocks, since it's located not in root
    if (type == "backend" && TerraformPatterns.Backend.accepts(block)) {
      return getBackendProperties(block)
    }
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
      "terraform" -> getTerraformProperties(block)
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
        val candidates = properties.filterIsInstance(BlockType::class.java).filter { it.literal == type }
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

  fun getBackendProperties(block: HCLBlock): Array<out PropertyOrBlockType> {
    val type = block.getNameElementUnquoted(1)
    val backendType = type?.let { getTypeModel(block.project).getBackendType(it) } ?: return emptyArray()
    return backendType.properties.toList().toTypedArray()
  }

  @Suppress("UNUSED_PARAMETER")
  fun getTerraformProperties(block: HCLBlock): Array<PropertyOrBlockType> {
    val base: Array<out PropertyOrBlockType> = TypeModel.Terraform.properties
    return (base.toList() + TypeModel.AbstractBackend).toTypedArray()
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
        properties.add(PropertyType(name, Types.String, required = !hasDefault))
      }
    }
    return (properties.toTypedArray())
  }


  fun getTypeModel(project: Project): TypeModel {
    return TypeModelProvider.getModel(project)
  }
}
