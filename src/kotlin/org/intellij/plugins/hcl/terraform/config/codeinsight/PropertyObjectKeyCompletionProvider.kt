/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.intellij.plugins.debug
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hcl.terraform.config.codeinsight.TerraformConfigCompletionContributor.Companion.getIncomplete
import org.intellij.plugins.hcl.terraform.config.model.Module

// Similar to BlockPropertiesCompletionProvider
object PropertyObjectKeyCompletionProvider : TerraformConfigCompletionContributor.OurCompletionProvider() {
  private val LOG = Logger.getInstance(PropertyObjectKeyCompletionProvider::class.java)

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    val position = parameters.position // DQS, SQS or ID
    val parent = position.parent // Literal or Identifier or Object
    if (parent is HCLStringLiteral || parent is HCLIdentifier) {
      val pp = parent.parent
      // Do not complete values
      if (pp is HCLProperty && pp.nameElement !== parent) return
    }
    val obj = PsiTreeUtil.getParentOfType(parent, HCLObject::class.java, false) ?: return
    LOG.debug { "TF.PropertyObjectKeyCompletionProvider{position=$position, parent=$parent, obj=$obj}" }
    when (obj.parent) {
      is HCLProperty -> {
        return addPropertyCompletions(parameters, result, obj)
      }
      is HCLBlock -> {
        return addInnerBlockCompletions(parameters, result, obj)
      }
      else -> return
    }
  }

  private fun addPropertyCompletions(parameters: CompletionParameters, result: CompletionResultSet, obj: HCLObject) {
    val property = obj.parent as? HCLProperty ?: return
    val block = PsiTreeUtil.getParentOfType(property, HCLBlock::class.java) ?: return
    LOG.debug { "TF.PropertyObjectKeyCompletionProvider.Property{block=$block, inner-property=$property}" }

    val type = block.getNameElementUnquoted(0)
    // TODO: Replace with 'ReferenceHint'
    if (property.name == "providers" && type == "module") {
      val module = Module.getAsModuleBlock(block) ?: return
      val incomplete: String? = getIncomplete(parameters)
      val defined = TerraformConfigCompletionContributor.getOriginalObject(parameters, obj).propertyList.map { it.name }
      val providers = module.getDefinedProviders()
          .map { it.second }
          .filter { !defined.contains(it) || (incomplete != null && it.contains(incomplete)) }
      result.addAllElements(providers.map { TerraformConfigCompletionContributor.create(it) })
      return
    }
  }

  private fun addInnerBlockCompletions(parameters: CompletionParameters, result: CompletionResultSet, obj: HCLObject) {
    val innerBlock = PsiTreeUtil.getParentOfType(obj, HCLBlock::class.java) ?: return
    val block = PsiTreeUtil.getParentOfType(innerBlock, HCLBlock::class.java, true) ?: return
    LOG.debug { "TF.PropertyObjectKeyCompletionProvider.Block{block=$block, inner-block=$block}" }

    val type = block.getNameElementUnquoted(0)
    // TODO: Replace with 'ReferenceHint'
    if (innerBlock.name == "providers" && type == "module") {
      val module = Module.getAsModuleBlock(block) ?: return
      val incomplete: String? = getIncomplete(parameters)
      val defined = TerraformConfigCompletionContributor.getOriginalObject(parameters, obj).propertyList.map { it.name }
      val providers = module.getDefinedProviders()
          .map { it.second }
          .filter { !defined.contains(it) || (incomplete != null && it.contains(incomplete)) }
      result.addAllElements(providers.map { TerraformConfigCompletionContributor.create(it) })
      return
    }
  }
}
