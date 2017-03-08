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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.impl.getHCLHost

interface IntroduceVariableSettings {
  val name: String
  val type: Type
  val isReplaceAllOccurrences: Boolean
  val isOK: Boolean
}

interface Validator {
  fun isOK(settings: IntroduceVariableSettings): Boolean
}

class InputValidator(private val myProject: Project, private val myAnchorStatement: ILExpression) : Validator {

  override fun isOK(settings: IntroduceVariableSettings): Boolean {
    val name = settings.name
    val variables = myAnchorStatement.getHCLHost()?.getTerraformModule()?.getAllVariables()?.filter { name == it.first.name } ?: return true
    if (variables.isEmpty()) {
      return true
    }
    val conflicts = MultiMap<PsiElement, String>()
    for (variable in variables) {
      conflicts.putValue(variable.second, CommonRefactoringUtil.capitalize(RefactoringBundle.message("introduced.variable.will.conflict.with.0", RefactoringUIUtil.getDescription(variable.second, false))))
    }
    if (conflicts.size() > 0) {
      return ILIntroduceVariableHandler.reportConflicts(conflicts, myProject)
    } else {
      return true
    }
  }
}