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
package org.intellij.plugins.hcl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.plugins.hcl.HCLElementTypes.*
import org.intellij.plugins.hcl.psi.impl.HCLFileImpl

open class HCLParserDefinition : ParserDefinition {

  override fun createLexer(project: Project): Lexer {
    return HCLLexer()
  }

  override fun createParser(project: Project): PsiParser {
    return HCLParser()
  }

  override fun getFileNodeType(): IFileElementType {
    return FILE
  }

  override fun getWhitespaceTokens(): TokenSet {
    return WHITE_SPACES
  }

  override fun getCommentTokens(): TokenSet {
    return HCL_COMMENTARIES
  }

  override fun getStringLiteralElements(): TokenSet {
    return STRING_LITERALS
  }

  override fun createElement(node: ASTNode): PsiElement {
    val type = node.elementType
    if (type is HCLElementType) {
      return Factory.createElement(node)
    }
    if (type is HCLTokenType) {
      return Factory.createElement(node)
    }
    return ASTWrapperPsiElement(node)
  }

  override fun createFile(fileViewProvider: FileViewProvider): PsiFile {
    return HCLFileImpl(fileViewProvider, HCLLanguage)
  }

  override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
    if (left.elementType == LINE_COMMENT) return ParserDefinition.SpaceRequirements.MUST_LINE_BREAK
    return ParserDefinition.SpaceRequirements.MAY
  }

  companion object {
    @JvmField val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    val STRING_LITERALS: TokenSet = TokenSet.create(SINGLE_QUOTED_STRING, DOUBLE_QUOTED_STRING)

    val IDENTIFYING_LITERALS: TokenSet = TokenSet.create(SINGLE_QUOTED_STRING, DOUBLE_QUOTED_STRING, ID)

    val FILE: IFileElementType = IFileElementType(HCLLanguage)

    val HCL_BRACES: TokenSet = TokenSet.create(L_CURLY, R_CURLY)
    val HCL_BRACKETS: TokenSet = TokenSet.create(L_BRACKET, R_BRACKET)
    val HCL_CONTAINERS: TokenSet = TokenSet.create(OBJECT, ARRAY)
    val HCL_BOOLEANS: TokenSet = TokenSet.create(TRUE, FALSE)
    val HCL_KEYWORDS: TokenSet = TokenSet.create(TRUE, FALSE, NULL)
    @JvmField val HCL_LITERALS: TokenSet = TokenSet.create(STRING_LITERAL, NUMBER_LITERAL, NULL_LITERAL, TRUE, FALSE)
    val HCL_VALUES: TokenSet = TokenSet.orSet(HCL_CONTAINERS, HCL_LITERALS)
    @JvmField val HCL_COMMENTARIES: TokenSet = TokenSet.create(BLOCK_COMMENT, LINE_COMMENT)
  }
}
