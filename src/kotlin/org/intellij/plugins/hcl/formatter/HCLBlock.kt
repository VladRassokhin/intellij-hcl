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
package org.intellij.plugins.hcl.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.plugins.hcl.HCLElementTypes.*
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.HCLPsiUtil

class HCLBlock(val parent: HCLBlock?, node: ASTNode, wrap: Wrap?, alignment: Alignment?, val spacingBuilder: SpacingBuilder, val _indent: Indent?, val settings: HCLCodeStyleSettings, private val valueAlignment: Alignment? = null) : AbstractBlock(node, wrap, alignment) {
  val myChildWrap: Wrap?

  init {
    myChildWrap = when (node.elementType) {
      OBJECT -> Wrap.createWrap(settings.OBJECT_WRAPPING, true)
      ARRAY -> Wrap.createWrap(settings.ARRAY_WRAPPING, true)
      else -> null
    }
  }

  val OPEN_BRACES: TokenSet = TokenSet.create(L_CURLY, L_BRACKET)
  val CLOSE_BRACES: TokenSet = TokenSet.create(R_CURLY, R_BRACKET)
  val ALL_BRACES: TokenSet = TokenSet.orSet(OPEN_BRACES, CLOSE_BRACES)

  override fun buildChildren(): MutableList<Block>? {
    var propertyValueAlignment: Alignment? =
        if (settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.DO_NOT_ALIGN_PROPERTY) null
        else if (isElementType(node, OBJECT) || isFile(node)) Alignment.createAlignment(true)
        else null
    return myNode.getChildren(null).map {
      if (it.elementType == TokenType.WHITE_SPACE && propertyValueAlignment != null) {
        val text = it.text
        val first = text.indexOf('\n')
        if (first >= 0 && text.indexOf('\n', first + 1) > 0) {
          propertyValueAlignment = Alignment.createAlignment(true)
        }
      }
      if (isWhitespaceOrEmpty(it)) null
      else makeSubBlock(it, propertyValueAlignment)
    }.filterNotNull().toMutableList()
  }

  private fun makeSubBlock(childNode: ASTNode, propertyValueAlignment: Alignment?): HCLBlock {
    var indent = Indent.getNoneIndent()
    var alignment: Alignment? = null
    var wrap: Wrap? = null

    if (isElementType(myNode, HCLParserDefinition.HCL_CONTAINERS)) {
      assert(myChildWrap != null) { "myChildWrap should not be null for container, ${myNode.elementType}" }

      if (isElementType(childNode, COMMA)) {
        wrap = Wrap.createWrap(WrapType.NONE, true)
      } else if (!isElementType(childNode, ALL_BRACES)) {
        wrap = myChildWrap!!
        indent = Indent.getNormalIndent()
      } else if (isElementType(childNode, OPEN_BRACES)) {
        if (HCLPsiUtil.isPropertyValue(myNode.psi) && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
          // WEB-13587 Align compound values on opening brace/bracket, not the whole block
          assert(valueAlignment != null)
          alignment = valueAlignment
        }
      }
    } else if (isElementType(myNode, PROPERTY)) {
      // Handle properties alignment
      val pva = valueAlignment
      if (isElementType(childNode, EQUALS) && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_EQUALS) {
        assert(pva != null) { "Expected not null PVA, node ${node.elementType}, parent ${parent?.node?.elementType}" }
        alignment = pva
      } else if (HCLPsiUtil.isPropertyValue(childNode.psi) && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
        assert(pva != null) { "Expected not null PVA, node ${node.elementType}, parent ${parent?.node?.elementType}" }
        if (!isElementType(childNode, HCLParserDefinition.HCL_CONTAINERS)) {
          // WEB-13587 Align compound values on opening brace/bracket, not the whole block
          alignment = pva
        }
      }
    } else if (isElementType(myNode, HEREDOC_LITERAL)) {
      if (this.textRange == myNode.textRange) {
        if (isElementType(childNode, HEREDOC_CONTENT, HEREDOC_MARKER, HD_LINE, HD_MARKER)) {
          wrap = Wrap.createWrap(WrapType.NONE, false)
          indent = Indent.getAbsoluteNoneIndent()
        } else if (isElementType(childNode, HD_START)) {
          wrap = Wrap.createWrap(WrapType.NONE, false)
          indent = Indent.getNoneIndent()
        }
      }
    }
    return HCLBlock(this, childNode, wrap, alignment, spacingBuilder, indent, settings, propertyValueAlignment ?: valueAlignment)
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
    return ChildAttributes(childIndent, getFirstChildAlignment(newChildIndex))
  }

  override fun getChildIndent(): Indent? {
    if (isElementType(myNode, HEREDOC_LITERAL, HEREDOC_MARKER, HEREDOC_CONTENT, HD_MARKER, HD_LINE, HD_START)) {
      return Indent.getAbsoluteNoneIndent()
    }
    if (isElementType(myNode, OBJECT)) {
      return Indent.getNormalIndent()
    }
    if (isElementType(myNode, ARRAY)) {
      return Indent.getNormalIndent()
    }
    if (isFile(myNode)) {
      return Indent.getNoneIndent()
    }
    return null
  }

  fun getFirstChildAlignment(newChildIndex: Int): Alignment? {
    if (isElementType(myNode, OBJECT) || isFile(myNode)) {
      return null
    }
    if (isElementType(myNode, PROPERTY)) {
      if (newChildIndex == 1 && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_EQUALS) {
        // equals
        return valueAlignment
      }
      if (newChildIndex == 2 && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
        // Property value
        return valueAlignment
      }
    }
    return null
  }

  private fun isElementType(node: ASTNode, set: TokenSet): Boolean {
    return set.contains(node.elementType)
  }

  private fun isElementType(node: ASTNode, vararg types: IElementType): Boolean {
    return types.contains(node.elementType)
  }

  private fun isFile(node: ASTNode): Boolean {
    return node.elementType is IFileElementType
  }

  private fun isWhitespaceOrEmpty(node: ASTNode): Boolean {
    return node.elementType == TokenType.WHITE_SPACE || node.textLength == 0
  }

  override fun getIndent(): Indent? {
    return _indent
  }

  override fun isLeaf(): Boolean {
    if (isElementType(myNode, HEREDOC_CONTENT, HEREDOC_MARKER, HD_LINE, HD_MARKER)) return true
    return myNode.firstChildNode == null
  }

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    return spacingBuilder.getSpacing(this, child1, child2)
  }
}
