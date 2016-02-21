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
package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.terraform.config.model.getTerraformSearchScope

abstract class HCLBlockMixin(node: ASTNode) : HCLElementImpl(node), HCLBlock {

  override fun getNameIdentifier(): PsiElement {
    return nameElements.last()!! // Block always have at least one nameElement
  }

  @Throws(IncorrectOperationException::class)
  override fun setName(name: String): PsiElement {
    // TODO: Should we change only nameIdentifier or other nameElement also?
    ElementChangeUtil.doNameReplacement(this, nameIdentifier, name, HCLElementTypes.IDENTIFIER)
    return this
  }

  // TODO: Add proper references (?)

  override fun getUseScope(): SearchScope {
    return this.getTerraformSearchScope()
  }
}
