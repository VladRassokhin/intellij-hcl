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
package org.intellij.plugins.hil.codeinsight

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import org.intellij.plugins.debug
import org.intellij.plugins.hcl.navigation.HCLQualifiedNameProvider
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.Constants
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionContributor
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionContributor.BlockTypeOrNameCompletionProvider.isProviderUsed
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformLookupElementRenderer
import org.intellij.plugins.hcl.terraform.config.model.*
import org.intellij.plugins.hcl.terraform.config.model.Function
import org.intellij.plugins.hil.HILLanguage
import org.intellij.plugins.hil.codeinsight.ReferenceCompletionHelper.findByFQNRef
import org.intellij.plugins.hil.psi.*
import org.intellij.plugins.hil.psi.impl.getHCLHost
import java.util.*

class HILCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, METHOD_POSITION, MethodsCompletionProvider)
    extend(CompletionType.BASIC, METHOD_POSITION, ResourceTypesCompletionProvider)
    extend(null, METHOD_POSITION, FullReferenceCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, ILSE_FROM_KNOWN_SCOPE)
        , KnownScopeCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, ILSE_NOT_FROM_KNOWN_SCOPE)
        , SelectCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, ILSE_DATA_SOURCE)
        , SelectCompletionProvider)

    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILLiteralExpression::class.java).withSuperParent(2, ILISE_NOT_FROM_KNOWN_SCOPE)
        , SelectCompletionProvider)
  }

  override fun beforeCompletion(context: CompletionInitializationContext) {
    context.dummyIdentifier = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
  }

  companion object {
    @JvmField val GLOBAL_SCOPES: SortedSet<String> = sortedSetOf("var", "path", "data", "module", "local")

    private fun getScopeSelectPatternCondition(scopes: Set<String>): PatternCondition<ILSelectExpression?> {
      return object : PatternCondition<ILSelectExpression?>("ScopeSelect($scopes)") {
        override fun accepts(t: ILSelectExpression, context: ProcessingContext?): Boolean {
          val from = t.from
          return from is ILVariable && from.name in scopes
        }
      }
    }

    private val SCOPE_PROVIDERS = listOf(
        DataSourceCompletionProvider,
        VariableCompletionProvider,
        SelfCompletionProvider,
        PathCompletionProvider,
        CountCompletionProvider,
        TerraformCompletionProvider,
        LocalsCompletionProvider,
        ModuleCompletionProvider
    ).map { it.scope to it }.toMap()
    val SCOPES = SCOPE_PROVIDERS.keys

    private val METHOD_POSITION = PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java)
        .andNot(PlatformPatterns.psiElement().withSuperParent(2, ILSelectExpression::class.java))

    val ILSE_FROM_KNOWN_SCOPE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .with(getScopeSelectPatternCondition(SCOPES))!!
    val ILSE_NOT_FROM_KNOWN_SCOPE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .without(getScopeSelectPatternCondition(SCOPES))!!
    val ILISE_NOT_FROM_KNOWN_SCOPE = PlatformPatterns.psiElement(ILIndexSelectExpression::class.java)
        .without(getScopeSelectPatternCondition(SCOPES))!!
    val ILSE_FROM_DATA_SCOPE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .with(getScopeSelectPatternCondition(setOf("data")))!!
    val ILSE_DATA_SOURCE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .with(object : PatternCondition<ILSelectExpression?>("ILSE_Data_Source()") {
          override fun accepts(t: ILSelectExpression, context: ProcessingContext?): Boolean {
            val from = t.from as? ILSelectExpression ?: return false
            return ILSE_FROM_DATA_SCOPE.accepts(from)
          }
        })!!


    private val LOG = Logger.getInstance(HILCompletionContributor::class.java)
    fun create(value: String): LookupElementBuilder {
      val builder = LookupElementBuilder.create(value)
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

    fun create(value: PropertyOrBlockType, lookupString: String? = null): LookupElementBuilder {
      var builder = LookupElementBuilder.create(lookupString ?: value.name)
      builder = builder.withRenderer(TerraformLookupElementRenderer())
      return builder
    }
  }

  private object MethodsCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent as? ILExpression ?: return
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug { "HIL.MethodsCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}" }
      result.addAllElements(TypeModelProvider.getModel(position.project).functions.values.map { create(it) })
      result.addAllElements(GLOBAL_SCOPES.map { createScope(it) })
      if (getProvisionerResource(parent) != null) result.addElement(createScope("self"))
      if (getResource(parent) != null || getDataSource(parent) != null) result.addElement(createScope("count"))
    }
  }

  private abstract class SelectFromScopeCompletionProvider(val scope: String) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent as? ILVariable ?: return
      val pp = parent.parent as? ILSelectExpression ?: return
      val from = pp.from as? ILVariable ?: return
      if (scope != from.name) return
      LOG.debug { "HIL.SelectFromScopeCompletionProvider($scope){position=$position, parent=$parent, pp=$pp}" }
      doAddCompletions(parent, parameters, context, result)
    }

    abstract fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet)
  }

  object KnownScopeCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent as? ILVariable ?: return
      val pp = parent.parent as? ILSelectExpression ?: return
      val from = pp.from as? ILVariable ?: return
      val provider = SCOPE_PROVIDERS[from.name] ?: return
      LOG.debug { "HIL.SelectFromScopeCompletionProviderAny($from.name){position=$position, parent=$parent, pp=$pp}" }
      provider.doAddCompletions(parent, parameters, context, result)
    }
  }

  private object VariableCompletionProvider : SelectFromScopeCompletionProvider("var") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val variables: List<Variable> = getLocalDefinedVariables(variable)
      for (v in variables) {
        result.addElement(create(v.name))
      }
    }
  }

  private object SelfCompletionProvider : SelectFromScopeCompletionProvider("self") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      // For now 'self' allowed only for provisioners inside resources

      val resource = getProvisionerResource(variable) ?: return
      val properties = ModelHelper.getBlockProperties(resource).filter { it.name != Constants.HAS_DYNAMIC_ATTRIBUTES }
      // TODO: Filter already defined or computed properties (?)
      // TODO: Add type filtration
      val set = properties.map { it.name }.toHashSet()
      val obj = resource.`object`
      if (obj != null) {
        set.addAll(obj.propertyList.map { it.name })
      }
      result.addAllElements(set.map { create(it) })
    }
  }

  private object PathCompletionProvider : SelectFromScopeCompletionProvider("path") {
    private val PATH_REFERENCES = sortedSetOf("root", "module", "cwd")

    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      result.addAllElements(PATH_REFERENCES.map { create(it) })
    }
  }

  private object CountCompletionProvider : SelectFromScopeCompletionProvider("count") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      getResource(variable) ?: getDataSource(variable) ?: return
      result.addElement(create("index"))
    }
  }

  private object TerraformCompletionProvider : SelectFromScopeCompletionProvider("terraform") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      getResource(variable) ?: getDataSource(variable) ?: return
      result.addElement(create("env"))
    }
  }

  private object LocalsCompletionProvider : SelectFromScopeCompletionProvider("local") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val variables: List<String> = getLocalDefinedLocals(variable)
      for (v in variables) {
        result.addElement(create(v))
      }
    }
  }

  private object ModuleCompletionProvider : SelectFromScopeCompletionProvider("module") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val module = getTerraformModule(variable) ?: return
      val modules = module.getDefinedModules()
      for (m in modules) {
        val name = m.getNameElementUnquoted(1)
        if (name != null) result.addElement(create(name))
      }
    }
  }

  private object DataSourceCompletionProvider : SelectFromScopeCompletionProvider("data") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val module = getTerraformModule(variable) ?: return

      val dataSources = module.getDeclaredDataSources()
      val types = dataSources.map { it.getNameElementUnquoted(1) }.filterNotNull().toSortedSet()
      result.addAllElements(types.map { create(it) })

      if (parameters.isExtendedCompletion) {
        @Suppress("NAME_SHADOWING")
        var dataSources = ModelHelper.getTypeModel(parameters.position.project).dataSources.values
        val cache = HashMap<String, Boolean>()
        if (parameters.invocationCount == 2) {
          dataSources = dataSources.filter { isProviderUsed(module, it.provider.type, cache) }
        }
        result.addAllElements(dataSources.map { it.type }.filter { it !in types }.map { create(it) })
      }
    }
  }

  private object SelectCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position

      val element = position.parent as? ILExpression ?: return
      if (element !is ILVariable && element !is ILLiteralExpression) return
      val host = element.getHCLHost() ?: return

      val parent = element.parent as? ILSelectExpression ?: return

      val expression = getGoodLeftElement(parent, element) ?: return
      val references = expression.references
      if (references.isNotEmpty()) {
        val resolved = SmartList<PsiElement>()
        for (reference in references) {
          if (reference is PsiPolyVariantReference) {
            resolved.addAll(reference.multiResolve(false).map { it.element }.filterNotNull())
          } else {
            reference.resolve()?.let { resolved.add(it) }
          }
        }

        val found = ArrayList<LookupElement>()
        for (r in resolved) {
          when (r) {
            is HCLStringLiteral, is HCLIdentifier ->{
              val p = r.parent
              if (p is HCLBlock) {
                getBlockProperties(p, found)
              } else if (p is HCLProperty && p.nameElement === r) {
                getPropertyObjectProperties(p, found)
              }
            }
            is HCLBlock -> {
              getBlockProperties(r, found)
            }
            is HCLProperty -> {
              getPropertyObjectProperties(r, found)
            }
          }
        }
        if (!found.isEmpty()) {
          result.addAllElements(found)
        }
        return
      }

      if (expression is ILVariable) {
        val module = host.getTerraformModule()
        val names = TreeSet<String>()
        if (HILCompletionContributor.ILSE_DATA_SOURCE.accepts(parent)) {
          val dataSources = module.findDataSource(expression.name, null)
          dataSources.map { it.getNameElementUnquoted(2) }.filterNotNull().toCollection(names)
        } else {
          val resources = module.findResources(expression.name, null)
          resources.map { it.getNameElementUnquoted(2) }.filterNotNull().toCollection(names)
        }
        result.addAllElements(names.map { create(it) })
        // TODO: support 'module.MODULE_NAME.OUTPUT_NAME' references (in that or another provider)
      }
    }

    private fun getPropertyObjectProperties(r: HCLProperty, found: ArrayList<LookupElement>) {
      val value = r.value
      if (value is HCLObject) {
        found.addAll(value.propertyList.map { create(it.name) })
      }
    }

    private fun getBlockProperties(r: HCLBlock, found: ArrayList<LookupElement>) {
      val type = r.getNameElementUnquoted(0)
      if (type == "variable") {
        val defaultMap = r.`object`?.findProperty("default")?.value
        if (defaultMap is HCLObject) {
          val names = HashSet<String>()
          defaultMap.propertyList.mapNotNullTo(names) { it.name }
          defaultMap.blockList.mapNotNullTo(names) { it.name }
          names.mapTo(found) { create(it) }
        }
        return
      } else if (type == "module") {
        val module = Module.getAsModuleBlock(r)
        if (module != null) {
          // TODO: Add special LookupElementRenderer
          module.getDefinedOutputs().map { create(it.name) }.toCollection(found)
        }
        return
      }
      val properties = ModelHelper.getBlockProperties(r).filter { it.name != Constants.HAS_DYNAMIC_ATTRIBUTES }
      val done = properties.map { it.name }.toSet()
      found.addAll(properties.map { create(it) })
      val pl = r.`object`?.propertyList
      if (pl != null) {
        found.addAll(pl.map { it.name }.filter { it !in done }.map { create(it) })
      }
    }

  }

  private object ResourceTypesCompletionProvider : TerraformConfigCompletionContributor.OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent as? ILExpression ?: return
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug { "HIL.ResourceTypesCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}" }

      val host = parent.getHCLHost() ?: return

      val module = host.getTerraformModule()
      val resources = module.getDeclaredResources()
      val types = resources.map { it.getNameElementUnquoted(1) }.filterNotNull().toSortedSet()
      result.addAllElements(types.map { create(it) })

      if (parameters.isExtendedCompletion) {
        @Suppress("NAME_SHADOWING")
        var resources = getTypeModel(position.project).resources.values
        val cache = HashMap<String, Boolean>()
        if (parameters.invocationCount == 2) {
          resources = resources.filter { isProviderUsed(module, it.provider.type, cache) }
        }
        result.addAllElements(resources.map { it.type }.filter { it !in types }.map { create(it) })
      }
    }
  }

  private object FullReferenceCompletionProvider : TerraformConfigCompletionContributor.OurCompletionProvider() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      if (parameters.completionType != CompletionType.SMART && !parameters.isExtendedCompletion) return
      val position = parameters.position
      val parent = position.parent as? ILExpression ?: return
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug { "HIL.ResourceTypesCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}" }

      val host = parent.getHCLHost() ?: return

      val property = PsiTreeUtil.getParentOfType(host, HCLProperty::class.java) ?: return
      val block = PsiTreeUtil.getParentOfType(property, HCLBlock::class.java) ?: return

      val props = ModelHelper.getBlockProperties(block).filterIsInstance(PropertyType::class.java)
      val hints = props.filter { it.name == property.name && it.hint != null }.map { it.hint }
      val hint = hints.firstOrNull() ?: return
      if (hint is ReferenceHint) {
        val module = property.getTerraformModule()
        hint.hint
            .mapNotNull { findByFQNRef(it, module) }
            .flatMap { it }
            .mapNotNull { it ->
              return@mapNotNull when (it) {
                is HCLBlock -> HCLQualifiedNameProvider.getQualifiedModelName(it)
                is HCLProperty -> HCLQualifiedNameProvider.getQualifiedModelName(it)
                is String -> it
                else -> null
              }
            }
            .forEach { result.addElement(create(it)) }
        return
      }
      // TODO: Support other hint types
    }
  }
}

