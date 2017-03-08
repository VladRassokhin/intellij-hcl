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
package org.intellij.plugins.hil.refactoring

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.impl.HILPsiImplUtils
import javax.swing.JComponent

class VariableInplaceIntroducer(project: Project,
                                val settings: IntroduceVariableSettings,
                                expr: ILExpression,
                                editor: Editor?,
                                val anchor: ILExpression,
                                occurrences: Array<ILExpression>,
                                title: String)
  : AbstractInplaceIntroducer<HCLBlock, ILExpression>(project, editor, expr, null, occurrences, title, TerraformFileType) {

  // TODO remove once other methods implemented
  override fun startInplaceIntroduceTemplate(): Boolean {
    return false
  }

  override fun getActionName(): String = "IntroduceVariable"

  override fun setReplaceAllOccurrences(allOccurrences: Boolean) = Unit
  override fun isReplaceAllOccurrences(): Boolean = settings.isReplaceAllOccurrences

  override fun getComponent(): JComponent? = null

  override fun suggestNames(replaceAll: Boolean, variable: HCLBlock?): Array<String> {
    val expr = expr ?: return emptyArray()
    return ILIntroduceVariableHandler.getSuggestedName(expr, HILPsiImplUtils.getType(expr)).names
  }

  override fun performIntroduce() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getVariable(): HCLBlock? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun restoreExpression(containingFile: PsiFile?, variable: HCLBlock?, marker: RangeMarker?, exprText: String?): ILExpression {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun createFieldToStartTemplateOn(replaceAll: Boolean, names: Array<out String>?): HCLBlock? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun saveSettings(variable: HCLBlock) {
  }

}