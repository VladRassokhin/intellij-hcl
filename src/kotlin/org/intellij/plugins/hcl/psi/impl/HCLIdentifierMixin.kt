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
package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.LeafElement
import org.intellij.plugins.hcl.psi.HCLElementGenerator
import org.intellij.plugins.hcl.psi.HCLIdentifier
import java.util.regex.Pattern

abstract class HCLIdentifierMixin(node: ASTNode) : HCLValueImpl(node), HCLIdentifier, PsiNamedElement {
  companion object {
    private val pattern = Pattern.compile("[a-zA-Z\\.\\-_][0-9a-zA-Z\\.\\-_]*")
  }

  override fun setName(s: String): PsiElement? {
    if (pattern.matcher(s).matches()) {
      // Safe to replace ast node
      (node.firstChildNode as LeafElement).replaceWithText(s);
      return this
    } else {
      // Replace identifier with string literal
      return HCLElementGenerator(project).createStringLiteral(s)
    }
  }

  override fun getName(): String? {
    return id
  }
}