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
package org.intellij.plugins.hcl.terraform.config.psi

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.patterns.HCLPatterns
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.externalDoc.*
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.TerraformConfigFile
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.TerraformVariablesFile
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns.propertyWithName
import org.intellij.plugins.hil.psi.HCLElementLazyReference
import org.intellij.plugins.hil.psi.HCLElementLazyReferenceBase

class TerraformReferenceContributor : PsiReferenceContributor() {
  companion object {
  }

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {


    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(psiElement(HCLProperty::class.java).with(object : PatternCondition<HCLProperty?>("HCLProperty(provider)") {
              override fun accepts(t: HCLProperty, context: ProcessingContext?): Boolean {
                return "provider" == t.name
              }
            }))
            .withSuperParent(3, psiElement(HCLBlock::class.java).with(object : PatternCondition<HCLBlock?>("HCLBlock(resource|data)") {
              override fun accepts(t: HCLBlock, context: ProcessingContext?): Boolean {
                val type = t.getNameElementUnquoted(0)
                return type == "resource" || type == "data"
              }
            }))
        , ResourceProviderReferenceProvider)

    // 'depends_on' in resources and data sources
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withSuperParent(1, psiElement(HCLArray::class.java))
            .withSuperParent(2, psiElement(HCLProperty::class.java).with(object : PatternCondition<HCLProperty?>("HCLProperty(depends_on)") {
              override fun accepts(t: HCLProperty, context: ProcessingContext?): Boolean {
                return "depends_on" == t.name
              }
            }))
            .withSuperParent(4, psiElement(HCLBlock::class.java).with(object : PatternCondition<HCLBlock?>("HCLBlock(resource|data)") {
              override fun accepts(t: HCLBlock, context: ProcessingContext?): Boolean {
                val type = t.getNameElementUnquoted(0)
                return type == "resource" || type == "data"
              }
            }))
        , DependsOnReferenceProvider)

    // Resolve variables usage in .tfvars
    registrar.registerReferenceProvider(
        psiElement(HCLIdentifier::class.java)
            .inFile(TerraformVariablesFile)
            .withParent(psiElement(HCLProperty::class.java))
        , VariableReferenceProvider)
    registrar.registerReferenceProvider(
        psiElement().withElementType(TokenSet.create(HCLElementTypes.IDENTIFIER, HCLElementTypes.STRING_LITERAL))
            .inFile(TerraformVariablesFile)
            .withSuperParent(1, HCLProperty::class.java)
            .withSuperParent(2, HCLObject::class.java)
            .withSuperParent(3, HCLProperty::class.java)
        , MapVariableIndexReferenceProvider)

    // 'module' source
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(propertyWithName("source"))
            .withSuperParent(3, TerraformPatterns.ModuleRootBlock)
        , ModuleSourceReferenceProvider)

    // 'module' variable setter
    registrar.registerReferenceProvider(
        psiElement(HCLIdentifier::class.java)
            .inFile(TerraformConfigFile)
            .withParent(psiElement(HCLProperty::class.java).with(object : PatternCondition<HCLProperty?>("HCLProperty(!source)") {
              override fun accepts(t: HCLProperty, context: ProcessingContext?): Boolean {
                return "source" != t.name
              }
            }))
            .withSuperParent(3, TerraformPatterns.ModuleRootBlock)
        , ModuleVariableReferenceProvider)

    // 'module' providers key/value
    registrar.registerReferenceProvider(
        psiElement().and(HCLPatterns.IdentifierOrStringLiteral)
            .inFile(TerraformConfigFile)
            .withParent(HCLPatterns.Property)
            .withSuperParent(2, HCLPatterns.Object)
            .withSuperParent(3, psiElement().and(HCLPatterns.PropertyOrBlock).andOr(propertyWithName("providers"), psiElement(HCLBlock::class.java).with(object: PatternCondition<HCLBlock?>("HCLBlock(providers)") {
              override fun accepts(t: HCLBlock, context: ProcessingContext?): Boolean {
                return t.nameElements.size == 1 && t.name == "providers"
              }
            })))
            .withSuperParent(4, HCLPatterns.Object)
            .withSuperParent(5, TerraformPatterns.ModuleRootBlock)
        , ModuleProvidersReferenceProvider)

    // keywords
    registrar.registerReferenceProvider(
        psiElement(HCLIdentifier::class.java)
            .inFile(TerraformConfigFile)
            .withParent(HCLPatterns.Block)
        , KeywordReferenceProvider)

    // 'backend' types
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(TerraformPatterns.Backend)
        , BackendTypeReferenceProvider)

    // 'data' types
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(TerraformPatterns.DataSourceRootBlock)
        , DataSourceTypeReferenceProvider)

    // 'provider' types
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(TerraformPatterns.ProviderRootBlock)
        , ProviderTypeReferenceProvider)

    // 'provisioner' types
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(TerraformPatterns.Provisioner)
        , ProvisionerTypeReferenceProvider)

    // 'resource' types
    registrar.registerReferenceProvider(
        psiElement(HCLStringLiteral::class.java)
            .inFile(TerraformConfigFile)
            .withParent(TerraformPatterns.ResourceRootBlock)
        , ResourceTypeReferenceProvider)
  }
}

// TODO: Fix renaming: add range to reference
object ResourceProviderReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is HCLStringLiteral) return PsiReference.EMPTY_ARRAY
    if (!HCLPsiUtil.isPropertyValue(element)) return PsiReference.EMPTY_ARRAY
    return arrayOf(HCLElementLazyReference(element, false) { incomplete, _ ->
      @Suppress("NAME_SHADOWING")
      val element = this.element
      if (incomplete) {
        element.getTerraformModule().getDefinedProviders().map { it.first.nameIdentifier as HCLElement }
      } else {
        element.getTerraformModule().findProviders(element.value).map { it.nameIdentifier as HCLElement }
      }
    })
  }
}

object ModuleSourceReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is HCLStringLiteral) return PsiReference.EMPTY_ARRAY
    if (!HCLPsiUtil.isPropertyValue(element)) return PsiReference.EMPTY_ARRAY
    return FileReferenceSet.createSet(element, true, false, false).allReferences
  }
}

// TODO: Fix renaming: add range to reference
object DependsOnReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is HCLStringLiteral) return PsiReference.EMPTY_ARRAY
    if (element.parent !is HCLArray) return PsiReference.EMPTY_ARRAY
    if (!HCLPsiUtil.isPropertyValue(element.parent)) return PsiReference.EMPTY_ARRAY
    return arrayOf(DependsOnLazyReference(element))
  }
}

class DependsOnLazyReference(element: HCLStringLiteral) : HCLElementLazyReferenceBase<HCLStringLiteral>(element, false) {
  override fun resolve(incompleteCode: Boolean, includeFake: Boolean): List<HCLElement> {
    val block = element.parent?.parent?.parent?.parent as? HCLBlock ?: return emptyList()

    var value = element.value
    val isDataSource = (value.startsWith("data."))
    if (isDataSource) value = value.removePrefix("data.")

    val module = element.getTerraformModule()
    if (incompleteCode) {
      val current = (if (block.getNameElementUnquoted(0) == "data") "data." else "") + "${block.getNameElementUnquoted(1)}.${block.name}"
      return module.getDeclaredResources().filter { "${it.getNameElementUnquoted(1)}.${it.name}" != current }
          .plus(module.getDeclaredDataSources().filter { "data.${it.getNameElementUnquoted(1)}.${it.name}" != current })
          .map { it.nameIdentifier as HCLElement }
    } else {
      val split = value.split('.')

      return if (split.size != 2) {
        emptyList()
      } else if (isDataSource) {
        module.findDataSource(split[0], split[1]).map { it.nameIdentifier as HCLElement }
      } else {
        module.findResources(split[0], split[1]).map { it.nameIdentifier as HCLElement }
      }
    }
  }

  fun getRangeInElementForRename(): TextRange {
    element.parent?.parent?.parent?.parent as? HCLBlock ?: return rangeInElement
    val value = element.value
    val split = value.split('.')
    if ((split.size == 3 && split[0] == "data") || split.size == 2) {
      val ll = split.last().length
      val tl = element.textLength
      return TextRange.create(tl - 1 - ll, tl - 1)
    }
    return rangeInElement
  }

  override fun handleElementRename(newElementName: String?): PsiElement {
    return ElementManipulators.getManipulator(element).handleContentChange(element, getRangeInElementForRename(), newElementName)
  }
}

object VariableReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is HCLIdentifier) return PsiReference.EMPTY_ARRAY
    if (!HCLPsiUtil.isPropertyKey(element)) return PsiReference.EMPTY_ARRAY

    val varReference = HCLElementLazyReference(element, false) { incomplete, _ ->
      @Suppress("NAME_SHADOWING")
      val element = this.element
      if (incomplete) {
        element.getTerraformModule().getAllVariables().map { it.second.nameIdentifier as HCLElement }
      } else {
        @Suppress("NAME_SHADOWING")
        val value = element.id
        listOf(element.getTerraformModule().findVariable(value.substringBefore('.'))?.second?.nameIdentifier as HCLElement?).filterNotNull()
      }
    }

    val value = element.id
    val dotIndex = value.indexOf('.')
    if (dotIndex != -1) {
      // Mapped variable
      // Two references: variable name (hard) and variable subvalue (soft)
      val subReference = HCLElementLazyReference(element, true) { incomplete, _ ->
        @Suppress("NAME_SHADOWING")
        val element = this.element
        @Suppress("NAME_SHADOWING")
        val value = element.id
        val variable = element.getTerraformModule().findVariable(value.substringBefore('.')) ?: return@HCLElementLazyReference emptyList()
        val default = variable.second.`object`?.findProperty("default")?.value as? HCLObject ?: return@HCLElementLazyReference emptyList()
        if (incomplete) {
          default.propertyList.map { it.nameElement }
        } else {
          listOf(default.findProperty(value.substringAfter('.'))?.nameElement).filterNotNull()
        }
      }
      varReference.rangeInElement = TextRange(0, dotIndex)
      subReference.rangeInElement = TextRange(dotIndex + 1, value.length)
      return arrayOf(varReference, subReference)
    }

    return arrayOf(varReference)
  }

}

object MapVariableIndexReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    if (element !is HCLElement) return PsiReference.EMPTY_ARRAY
    if (element !is HCLIdentifier && element !is HCLStringLiteral) return PsiReference.EMPTY_ARRAY

    if (!HCLPsiUtil.isPropertyKey(element)) return PsiReference.EMPTY_ARRAY

    val pObj = element.parent.parent as? HCLObject ?: return PsiReference.EMPTY_ARRAY

    if (pObj.parent !is HCLProperty) return PsiReference.EMPTY_ARRAY

    val subReference = HCLElementLazyReference(element, true) { incomplete, _ ->
      @Suppress("NAME_SHADOWING")
      val element = this.element
      if (element.parent?.parent?.parent !is HCLProperty) {
        return@HCLElementLazyReference emptyList()
      }
      val value = (element.parent?.parent?.parent as HCLProperty).name
      val variable = element.getTerraformModule().findVariable(value)?.second ?: return@HCLElementLazyReference emptyList()
      val default = variable.`object`?.findProperty("default")?.value as? HCLObject ?: return@HCLElementLazyReference emptyList()
      if (incomplete) {
        default.propertyList.map { it.nameElement }
      } else {
        listOfNotNull(element.name?.let { default.findProperty(it) }?.nameElement)
      }
    }
    return arrayOf(subReference)
  }
}
