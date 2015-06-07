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
package org.intellij.plugins.hcl.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.plugins.hcl.HCLElementTypes.*
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.HCLArray
import org.intellij.plugins.hcl.psi.HCLFile
import org.intellij.plugins.hcl.psi.HCLObject
import org.intellij.plugins.hcl.psi.HCLPsiUtil

class HCLBlock(val parent: HCLBlock?, node: ASTNode, wrap: Wrap?, alignment: Alignment?, val spacingBuilder: SpacingBuilder, val _indent: Indent?, val settings: CodeStyleSettings) : AbstractBlock(node, wrap, alignment) {
  val myChildWrap: Wrap?
  val myPropertyValueAlignment: Alignment?

  init {
    val psi = node.getPsi()
    myPropertyValueAlignment = if (psi is HCLObject || psi is HCLFile) Alignment.createAlignment(true) else null
    myChildWrap = when (psi) {
      is HCLObject -> Wrap.createWrap(getCustomSettings().OBJECT_WRAPPING, true)
      is HCLArray -> Wrap.createWrap(getCustomSettings().ARRAY_WRAPPING, true)
      else -> null
    }
  }

  public val OPEN_BRACES: TokenSet = TokenSet.create(L_CURLY, L_BRACKET)
  public val CLOSE_BRACES: TokenSet = TokenSet.create(R_CURLY, R_BRACKET)
  public val ALL_BRACES: TokenSet = TokenSet.orSet(OPEN_BRACES, CLOSE_BRACES)

  override fun buildChildren(): MutableList<Block>? {
    return myNode.getChildren(null).map {
      if (isWhitespaceOrEmpty(it)) null
      else makeSubBlock(it)
    }.filterNotNull().toArrayList();
  }

  private fun makeSubBlock(childNode: ASTNode): HCLBlock {
    var indent = Indent.getNoneIndent()
    var alignment: Alignment? = null
    var wrap: Wrap? = null

    val customSettings = getCustomSettings()
    if (isElementType(myNode, HCLParserDefinition.HCL_CONTAINERS)) {
      assert(myChildWrap != null, "myChildWrap should not be null for container, ${myNode.getElementType()}")

      if (isElementType(childNode, COMMA)) {
        wrap = Wrap.createWrap(WrapType.NONE, true)
      } else if (!isElementType(childNode, ALL_BRACES)) {
        wrap = myChildWrap!!
        indent = Indent.getNormalIndent()
      } else if (isElementType(childNode, OPEN_BRACES)) {
        if (HCLPsiUtil.isPropertyValue(myNode.getPsi()) && customSettings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
          // WEB-13587 Align compound values on opening brace/bracket, not the whole block
          assert(parent != null && parent.parent != null && parent.parent.myPropertyValueAlignment != null)
          alignment = parent!!.parent!!.myPropertyValueAlignment
        }
      }
    } else if (isElementType(myNode, PROPERTY)) {
      // Handle properties alignment
      assert(parent != null)
      parent!!;
      val pva = parent.myPropertyValueAlignment
      assert(pva != null, "Expected not null PVA, node ${getNode().getElementType()}, parent ${parent.getNode().getElementType()}")
      if (isElementType(childNode, EQUALS) && customSettings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_EQUALS) {
        alignment = pva
      } else if (HCLPsiUtil.isPropertyValue(childNode.getPsi()) && customSettings.PROPERTY_ALIGNMENT == HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE) {
        if (!isElementType(childNode, HCLParserDefinition.HCL_CONTAINERS)) {
          alignment = pva
        }
      }
    }
    return HCLBlock(this, childNode, wrap, alignment, spacingBuilder, indent, settings)
  }


  private fun getCustomSettings(): HCLCodeStyleSettings {
    return settings.getCustomSettings(javaClass<HCLCodeStyleSettings>())
  }

  private fun isElementType(node: ASTNode, set: TokenSet): Boolean {
    return set.contains(node.getElementType())
  }

  private fun isElementType(node: ASTNode, vararg types: IElementType): Boolean {
    return types.contains(node.getElementType())
  }

  private fun isWhitespaceOrEmpty(node: ASTNode): Boolean {
    return node.getElementType() == TokenType.WHITE_SPACE || node.getTextLength() == 0
  }

  override fun getIndent(): Indent? {
    return _indent;
  }

  override fun isLeaf(): Boolean {
    return myNode.getFirstChildNode() == null
  }

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    return spacingBuilder.getSpacing(this, child1, child2)
  }
}