/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.SmartList
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionContributor
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformLookupElementRenderer
import org.intellij.plugins.hcl.terraform.config.model.*
import org.intellij.plugins.hcl.terraform.config.model.Function
import org.intellij.plugins.hil.HILLanguage
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.ILSelectExpression
import org.intellij.plugins.hil.psi.ILVariable
import org.intellij.plugins.hil.psi.getGoodLeftElement
import java.util.*

class HILCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, METHOD_POSITION, MethodsCompletionProvider)
    extend(CompletionType.BASIC, METHOD_POSITION, ResourceTypesCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, ILSE_FROM_KNOWN_SCOPE)
        , KnownScopeCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, ILSE_NOT_FROM_KNOWN_SCOPE)
        , SelectCompletionProvider)
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java).withSuperParent(2, ILSE_DATA_SOURCE)
        , SelectCompletionProvider)
  }


  companion object {
    @JvmField val GLOBAL_SCOPES: SortedSet<String> = sortedSetOf("var", "path")
    @JvmField val FUNCTIONS = ServiceManager.getService(TypeModelProvider::class.java).get().functions

    // For tests purposes
    @JvmField val GLOBAL_AVAILABLE: SortedSet<String> = FUNCTIONS.map { it.name }.toMutableList().plus(GLOBAL_SCOPES).toSortedSet()


    private val PATH_REFERENCES = sortedSetOf("root", "module", "cwd")
    private val SCOPE_PROVIDERS = mapOf(
        Pair("data", DataSourceCompletionProvider),
        Pair("var", VariableCompletionProvider),
        Pair("self", SelfCompletionProvider),
        Pair("path", PathCompletionProvider),
        Pair("count", CountCompletionProvider),
        Pair("module", ModuleCompletionProvider)
    )
    val SCOPES = SCOPE_PROVIDERS.keys

    private val METHOD_POSITION = PlatformPatterns.psiElement().withLanguage(HILLanguage)
        .withParent(ILVariable::class.java)
        .andNot(PlatformPatterns.psiElement().withSuperParent(2, ILSelectExpression::class.java))

    val ILSE_FROM_KNOWN_SCOPE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .with(getScopeSelectPatternCondition(SCOPE_PROVIDERS.keys))
    val ILSE_NOT_FROM_KNOWN_SCOPE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .without(getScopeSelectPatternCondition(SCOPE_PROVIDERS.keys))
    val ILSE_FROM_DATA_SCOPE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .with(getScopeSelectPatternCondition(setOf("data")))
    val ILSE_DATA_SOURCE = PlatformPatterns.psiElement(ILSelectExpression::class.java)
        .with(object : PatternCondition<ILSelectExpression?>("ILSE_Data_Source()") {
          override fun accepts(t: ILSelectExpression, context: ProcessingContext?): Boolean {
            val from = t.from
            if (from !is ILSelectExpression) return false
            return ILSE_FROM_DATA_SCOPE.accepts(from)
          }
        })


    private val LOG = Logger.getInstance(HILCompletionContributor::class.java)
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

    fun create(value: PropertyOrBlockType, lookupString: String? = null): LookupElementBuilder {
      var builder = LookupElementBuilder.create(lookupString ?: value.name)
      builder = builder.withRenderer(TerraformLookupElementRenderer())
      return builder
    }
  }

  private object MethodsCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      if (parent !is ILExpression) return
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug("HIL.MethodsCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}")
      result.addAllElements(FUNCTIONS.map { create(it) })
      result.addAllElements(GLOBAL_SCOPES.map { createScope(it) })
      if (getProvisionerResource(parent) != null) result.addElement(createScope("self"))
      if (getResource(parent) != null || getDataSource(parent) != null) result.addElement(createScope("count"))
    }
  }

  private abstract class SelectFromScopeCompletionProvider(val scope: String) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      if (parent !is ILVariable) return
      val pp = parent.parent
      if (pp !is ILSelectExpression) return
      val from = pp.from
      if (from !is ILVariable) return
      if (scope != from.name) return
      LOG.debug("HIL.SelectFromScopeCompletionProvider($scope){position=$position, parent=$parent, pp=$pp}")
      doAddCompletions(parent, parameters, context, result)
    }

    abstract fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet)
  }

  object KnownScopeCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position
      val parent = position.parent
      if (parent !is ILVariable) return
      val pp = parent.parent
      if (pp !is ILSelectExpression) return
      val from = pp.from
      if (from !is ILVariable) return
      val provider = SCOPE_PROVIDERS[from.name] ?: return
      LOG.debug("HIL.SelectFromScopeCompletionProviderAny($from.name){position=$position, parent=$parent, pp=$pp}")
      provider.doAddCompletions(parent, parameters, context, result)
    }
  }

  private object VariableCompletionProvider : SelectFromScopeCompletionProvider("var") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val variables: List<Variable> = getLocalDefinedVariables(variable);
      for (v in variables) {
        result.addElement(create(v.name))
      }
    }
  }

  private object SelfCompletionProvider : SelectFromScopeCompletionProvider("self") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      // For now 'self' allowed only for provisioners inside resources

      val resource = getProvisionerResource(variable) ?: return
      val properties = ModelHelper.getBlockProperties(resource)
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

  private object ModuleCompletionProvider : SelectFromScopeCompletionProvider("module") {
    override fun doAddCompletions(variable: ILVariable, parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val module = getTerraformModule(variable) ?: return
      val modules = module.getDefinedModules();
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
        result.addAllElements(ModelHelper.getTypeModel().dataSources.map { it.type }.filter { it !in types }.map { create(it) })
      }
    }
  }

  private object SelectCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
      val position = parameters.position

      val element = position.parent
      if (element !is ILVariable) return
      val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return
      if (host !is HCLElement) return

      val parent = element.parent
      if (parent !is ILSelectExpression) return

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
        val resources = module.findResources(expression.name, null)
        val dataSources = module.findDataSource(expression.name, null)
        val resourceNames = resources.map { it.getNameElementUnquoted(2) }.filterNotNull()
        val dataSourceNames = dataSources.map { it.getNameElementUnquoted(2) }.filterNotNull()
        val names = (resourceNames + dataSourceNames).toSortedSet()
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
      if (r.getNameElementUnquoted(0) == "variable") {
        val defaultMap = r.`object`?.findProperty("default")?.value
        if (defaultMap is HCLObject) {
          val names = HashSet<String>()
          defaultMap.propertyList.mapNotNullTo(names) { it.name }
          defaultMap.blockList.mapNotNullTo(names) { it.name }
          names.mapTo(found) { create(it) }
        }
        return
      }
      val properties = ModelHelper.getBlockProperties(r)
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
      val parent = position.parent
      if (parent !is ILExpression) return
      val leftNWS = position.getPrevSiblingNonWhiteSpace()
      LOG.debug("HIL.ResourceTypesCompletionProvider{position=$position, parent=$parent, left=${position.prevSibling}, lnws=$leftNWS}")

      val host = InjectedLanguageManager.getInstance(parent.project).getInjectionHost(parent) ?: return
      if (host !is HCLElement) return

      val module = host.getTerraformModule()
      val resources = module.getDeclaredResources()
      val types = resources.map { it.getNameElementUnquoted(1) }.filterNotNull().toSortedSet()
      result.addAllElements(types.map { create(it) })

      if (parameters.isExtendedCompletion) {
        result.addAllElements(getTypeModel().resources.map { it.type }.filter { it !in types }.map { create(it) })
      }
    }
  }
}

fun getTerraformModule(element: ILExpression): Module? {
  val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return null
  if (host !is HCLElement) return null
  val module = host.getTerraformModule()
  return module
}

fun getLocalDefinedVariables(element: ILExpression): List<Variable> {
  return getTerraformModule(element)?.getAllVariables()?.map { it.first } ?: emptyList()
}

fun getProvisionerResource(position: ILExpression): HCLBlock? {
  val host = InjectedLanguageManager.getInstance(position.project).getInjectionHost(position) ?: return null

  // For now 'self' allowed only for provisioners inside resources
  return if (host is HCLElement) getProvisionerResource(host) else null
}

fun getProvisionerResource(host: HCLElement): HCLBlock? {
  val provisioner = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java) ?: return null
  if (provisioner.getNameElementUnquoted(0) == "connection") return getProvisionerResource(provisioner)
  if (provisioner.getNameElementUnquoted(0) != "provisioner") return null
  val resource = PsiTreeUtil.getParentOfType(provisioner, HCLBlock::class.java, true) ?: return null
  if (resource.getNameElementUnquoted(0) != "resource") return null
  return resource
}

fun getResource(position: ILExpression): HCLBlock? {
  val host = InjectedLanguageManager.getInstance(position.project).getInjectionHost(position) ?: return null

  // For now 'self' allowed only for provisioners inside resources

  val resource = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java, true) ?: return null
  if (resource.getNameElementUnquoted(0) != "resource") return null
  return resource
}

fun getDataSource(position: ILExpression): HCLBlock? {
  val host = InjectedLanguageManager.getInstance(position.project).getInjectionHost(position) ?: return null

  val dataSource = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java, true) ?: return null
  if (dataSource.getNameElementUnquoted(0) != "data") return null
  return dataSource
}

private fun getScopeSelectPatternCondition(scopes: Set<String>): PatternCondition<ILSelectExpression?> {
  return object : PatternCondition<ILSelectExpression?>("ScopeSelect($scopes)") {
    override fun accepts(t: ILSelectExpression, context: ProcessingContext?): Boolean {
      val from = t.from
      return from is ILVariable && from.name in scopes
    }
  }
}
