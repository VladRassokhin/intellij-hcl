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
package org.intellij.plugins.hcl.terraform.il.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.intellij.plugins.hcl.psi.HCLBlock

class HCLBlockNameReference(from: PsiElement, soft: Boolean, val block: HCLBlock?, val index: Int) : PsiReferenceBase<PsiElement>(from, soft) {
  override fun resolve(): PsiElement? {
    val elements = block?.nameElements ?: return null
    return elements[index]
    //    return block
  }

  override fun getVariants(): Array<out Any> {
    return EMPTY_ARRAY
  }
}