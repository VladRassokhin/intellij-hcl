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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.util.IncorrectOperationException
import org.intellij.plugins.hcl.psi.HCLElementGenerator
import org.intellij.plugins.hcl.psi.HCLIdentifier
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.jetbrains.annotations.NonNls

abstract class HCLPropertyMixin(node: ASTNode) : HCLElementImpl(node), HCLProperty {

  @Throws(IncorrectOperationException::class)
  override fun setName(@NonNls name: String): PsiElement {
    val generator = HCLElementGenerator(project)
    // Strip only both quotes in case user wants some exotic name like key'
    val element = nameElement
    val rep:PsiElement;
    if (element is HCLStringLiteral) {
      rep = generator.createStringLiteral(StringUtil.unquoteString(name))
    } else if (element is HCLIdentifier) {
      rep = generator.createIdentifier(StringUtil.unquoteString(name));
    } else {
      throw IllegalStateException("Unexpected property name element type ${element.javaClass.name}")
    }
    element.replace(rep)
    return this
  }

  override fun getReference(): PsiReference? {
    return references.firstOrNull()
  }

  override fun getReferences(): Array<PsiReference> {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this)
  }

  override fun getNameIdentifier(): PsiElement? {
    return nameElement
  }
}
