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
package org.intellij.plugins.hcl.terraform.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.terraform.config.patterns.TerraformPatterns

class TerraformRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(leaf: PsiElement): Info? {
    if (!HCLParserDefinition.IDENTIFYING_LITERALS.contains(leaf.node?.elementType)) return null

    val identifier = leaf.parent ?: return null

    val block = identifier.parent as? HCLBlock ?: return null

    if (block.nameIdentifier !== identifier) return null

    if (!TerraformPatterns.ResourceRootBlock.accepts(block)) return null

    TerraformResourceConfigurationProducer.getResourceTarget(block) ?: return null

    val actions = ExecutorAction.getActions(0)
    val tooltipProvider: Function<PsiElement, String> = Function { psiElement ->
      @Suppress("UselessCallOnCollection")
      actions.filterNotNull().map { getText(it, psiElement) }.filterNotNull().joinToString("\n")
    }
    return Info(AllIcons.RunConfigurations.TestState.Run, tooltipProvider, *actions)
  }

  companion object {
    private fun getText(action: AnAction, element: PsiElement): String? {
      val parent = DataManager.getInstance().dataContext
      val dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.name, element, parent)
      val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.STATUS_BAR_PLACE, dataContext)
      action.update(event)
      val presentation = event.presentation
      return if (presentation.isEnabled && presentation.isVisible) presentation.text else null
    }
  }
}