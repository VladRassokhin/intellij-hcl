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
  val myAlwaysWrap: Wrap?
  var myLastValueAlignment: Alignment? = null
  var myLastValueCommentAlignment: Alignment? = null

  init {
    myChildWrap = when (node.elementType) {
      OBJECT -> Wrap.createWrap(settings.OBJECT_WRAPPING, true)
      ARRAY -> Wrap.createWrap(settings.ARRAY_WRAPPING, true)
      else -> null
    }
    myAlwaysWrap = Wrap.createWrap(WrapType.ALWAYS, true)
  }

  override fun buildChildren(): MutableList<Block>? {
    var propertyValueAlignment: Alignment? =
        if (settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.DO_NOT_ALIGN_PROPERTY) null
        else if (isElementType(node, OBJECT) || isFile(node)) Alignment.createAlignment(true)
        else null
    return myNode.getChildren(null).mapNotNull {
      if (it.elementType == TokenType.WHITE_SPACE && propertyValueAlignment != null) {
        val text = it.text
        val first = text.indexOf('\n')
        if (first >= 0 && text.indexOf('\n', first + 1) > 0) {
          propertyValueAlignment = Alignment.createAlignment(true)
          myLastValueAlignment = null
          myLastValueCommentAlignment = null
        }
      }
      if (it.elementType !== TokenType.WHITE_SPACE) {
        if (it.textContains('\n') || (isElementType(it, HCLParserDefinition.HCL_COMMENTARIES) && it.treePrev?.textContains('\n') == true)) {
          myLastValueAlignment = null
          myLastValueCommentAlignment = null
        }
      }
      if (isWhitespaceOrEmpty(it)) null
      else makeSubBlock(it, propertyValueAlignment)
    }.toMutableList()
  }

  private fun getLastPropertyAlignment(): Alignment? {
    if (myLastValueAlignment == null) {
      myLastValueAlignment =
          if (settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.DO_NOT_ALIGN_PROPERTY) null
          else if (isElementType(node, OBJECT) || isFile(node)) Alignment.createAlignment(true)
          else null
    }
    return myLastValueAlignment
  }

  private fun getLastCommentAlignment(): Alignment? {
    if (myLastValueCommentAlignment == null) {
      myLastValueCommentAlignment = if (isElementType(myNode, OBJECT, ARRAY)) Alignment.createAlignment(true) else null
    }
    return myLastValueCommentAlignment
  }


  private fun makeSubBlock(childNode: ASTNode, propertyValueAlignment: Alignment?): HCLBlock {
    var indent = Indent.getNoneIndent()
    var alignment: Alignment? = null
    var wrap: Wrap? = null

    if (isElementType(myNode, HCLParserDefinition.HCL_CONTAINERS)) {
      assert(myChildWrap != null) { "myChildWrap should not be null for container, ${myNode.elementType}" }

      if (isElementType(childNode, COMMA)) {
        // The only case for wrapping - previous element is Heredoc
        wrap = Wrap.createWrap(WrapType.NONE, true)
        indent = Indent.getNormalIndent()
      } else if (isElementType(childNode, HCLParserDefinition.HCL_COMMENTARIES)) {
        if (isElementType(myNode, ARRAY)) {
          // Check if comment either standalone or attached to element
          if (isStandaloneComment(childNode)) {
            wrap = myAlwaysWrap
            indent = Indent.getNormalIndent()
          } else {
            if (isOnSameLineAsFirstChildrenOfParent(childNode)) {
              alignment = Alignment.createAlignment(true)
            } else {
              alignment = getLastCommentAlignment()
            }
          }
        } else if (!isStandaloneComment(childNode)){
          alignment = getLastCommentAlignment()
          indent = Indent.getNormalIndent()
        } else {
          indent = Indent.getNormalIndent()
        }
      } else if (!isElementType(childNode, ALL_BRACES)) {
        wrap = myChildWrap!!
        if (myNode.elementType == ARRAY) {
          // Check whether that children located on the same line as open bracket
          if (!isOnSameLineAsFirstChildrenOfParent(childNode)) {
            wrap = myAlwaysWrap
          }
        }
        indent = Indent.getNormalIndent()
      } else if (isElementType(childNode, OPEN_BRACES)) {
        if (HCLPsiUtil.isPropertyValue(myNode.psi) && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
          // WEB-13587 Align compound values on opening brace/bracket, not the whole block
          alignment = valueAlignment
        }
      } else if (isElementType(childNode, CLOSE_BRACES)) {
        if (isEmptyObject(myNode)) {
          wrap = Wrap.createWrap(WrapType.NONE, false)
        } else if (!isOnSameLineAsFirstChildrenOfParent(childNode)) {
          wrap = myAlwaysWrap
        }
      }
    } else if (isElementType(myNode, PROPERTY)) {
      // Handle properties alignment
      assert(valueAlignment != null)
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
    var pva = propertyValueAlignment
    if (isElementType(childNode, PROPERTY)) {
      pva = getLastPropertyAlignment()
    }
    val block = HCLBlock(this, childNode, wrap, alignment, spacingBuilder, indent, settings, pva ?: valueAlignment)
    return block
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
        return myLastValueAlignment
      }
      if (newChildIndex == 2 && settings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
        // Property value
        return myLastValueAlignment
      }
    }
    return null
  }

  companion object {
    private val OPEN_BRACES: TokenSet = TokenSet.create(L_CURLY, L_BRACKET)
    private val CLOSE_BRACES: TokenSet = TokenSet.create(R_CURLY, R_BRACKET)
    private val ALL_BRACES: TokenSet = TokenSet.orSet(OPEN_BRACES, CLOSE_BRACES)

    private fun isEmptyObject(node: ASTNode): Boolean {
      if (!isElementType(node, OBJECT)) {
        return false
      }
      var it: ASTNode? = node.firstChildNode
      while (it != null) {
        if (!isElementType(it, L_CURLY, R_CURLY, TokenType.WHITE_SPACE)) return false
        it = it.treeNext
      }
      return true
    }

    private fun isStandaloneComment(childNode: ASTNode): Boolean {
      var node: ASTNode? = childNode.treePrev
      while (node != null) {
        if (node.elementType == TokenType.WHITE_SPACE) {
          if (node.textContains('\n')) return true
        } else {
          return isElementType(node, TokenSet.orSet(OPEN_BRACES, HCLParserDefinition.HCL_COMMENTARIES))
        }
        node = node.treePrev
      }
      return false
    }

    private fun isOnSameLineAsFirstChildrenOfParent(childNode: ASTNode): Boolean {
      var node: ASTNode? = childNode.treePrev
      while (node != null) {
        if (node.elementType == TokenType.WHITE_SPACE) {
          if (node.textContains('\n')) return false
        }
        node = node.treePrev
      }
      return true
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
  }
  override fun getIndent(): Indent? {
    return _indent
  }

  override fun isLeaf(): Boolean {
    if (isElementType(myNode, HEREDOC_CONTENT, HEREDOC_MARKER, HD_LINE, HD_MARKER)) return true
    return myNode.firstChildNode == null
  }

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    if (child1 !is ASTBlock || child2 !is ASTBlock) return null
    val first = isElementType(child1.node, PROPERTY) && child1.node.textContains('\n')
        || isElementType(child1.node, HCLParserDefinition.HCL_COMMENTARIES) && !isStandaloneComment(child1.node)
    val child2IsMultiLineProperty = isElementType(child2.node, PROPERTY) && child2.node.textContains('\n')
    val child2IsMultiLineBlock = isElementType(child2.node, BLOCK) && child2.node.textContains('\n')
    val second = child2IsMultiLineProperty || child2IsMultiLineBlock
        || isElementType(child2.node, HCLParserDefinition.HCL_COMMENTARIES) && isStandaloneComment(child2.node)
    val third = child2IsMultiLineProperty && !isElementType(child1.node, L_CURLY)
    if (!isFile(myNode) && first && second || third) {
      return Spacing.createSpacing(0, 0, 2, true, 2)
    }
    return spacingBuilder.getSpacing(this, child1, child2)
  }
}
