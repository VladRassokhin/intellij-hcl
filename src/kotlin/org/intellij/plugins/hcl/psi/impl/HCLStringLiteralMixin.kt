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
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.model.getTerraformSearchScope

abstract class HCLStringLiteralMixin(node: ASTNode?) : HCLLiteralImpl(node), HCLStringLiteral, PsiLanguageInjectionHost, PsiNamedElement {
  override fun isValidHost() = true

  override fun updateText(s: String): HCLStringLiteralMixin {
    assert(s.length >= 2)
    val quote = s[0]
    assert(quote == s[s.lastIndex])
    assert(quote == '\'' || quote == '"')
    val buffer = StringBuilder(s)

    // TODO: Use HIL-aware string escaper (?)

    // Fix quotes
    if (node.elementType == HCLElementTypes.SINGLE_QUOTED_STRING) {
      if (quote != '\'') {
        buffer[0] = '\''
        buffer[buffer.lastIndex] = '\''
      }
    } else {
      if (quote != '"') {
        buffer[0] = '\"'
        buffer[buffer.lastIndex] = '\"'
      }
    }
    (node.firstChildNode as LeafElement).replaceWithText(buffer.toString())
    return this
  }

  override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
    return LiteralTextEscaper.createSimple(this)
  }

  override fun getName(): String? {
    return this.value
  }

  override fun setName(s: String): HCLStringLiteralMixin {
    val buffer = StringBuilder(s.length)
    // TODO: Use HIL-aware string escaper (?)
    if (node.elementType == HCLElementTypes.SINGLE_QUOTED_STRING) {
      buffer.append('\'')
      StringUtil.escapeStringCharacters(s.length, s, "\'", buffer)
      buffer.append('\'')
    } else {
      buffer.append('\"')
      StringUtil.escapeStringCharacters(s.length, s, "\"", buffer)
      buffer.append('\"')
    }
    return updateText(buffer.toString())
  }

  override fun getUseScope(): SearchScope {
    return this.getTerraformSearchScope()
  }

  override fun getResolveScope(): GlobalSearchScope {
    return this.getTerraformSearchScope()
  }
}
