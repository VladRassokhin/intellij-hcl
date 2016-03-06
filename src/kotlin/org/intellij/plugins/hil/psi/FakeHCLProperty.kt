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
package org.intellij.plugins.hil.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.psi.HCLValue

class FakeHCLProperty(val _name: String, val _parent: PsiElement) : FakePsiElement(), HCLProperty {
  override fun getName(): String {
    return _name
  }

  override fun getNameElement(): HCLValue {
    throw UnsupportedOperationException()
  }

  override fun getValue(): HCLValue? {
    throw UnsupportedOperationException()
  }

  override fun getParent(): PsiElement? {
    return _parent
  }

  override fun getNameIdentifier(): PsiElement? {
    throw UnsupportedOperationException()
  }

}
