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
package org.intellij.plugins.hcl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore
import org.intellij.plugins.hcl.HCLElementTypes.*
import org.intellij.plugins.hcl.psi.HCLLexer
import org.intellij.plugins.hcl.psi.impl.HCLFileImpl
import org.jetbrains.lang.manifest.psi.ManifestElementType

public open class HCLParserDefinition : ParserDefinition {

  override fun createLexer(project: Project) = HCLLexer()

  override fun createParser(project: Project) = HCLParser()

  override fun getFileNodeType() = FILE

  override fun getWhitespaceTokens() = WHITE_SPACES

  override fun getCommentTokens() = HCL_COMMENTARIES

  override fun getStringLiteralElements() = STRING_LITERALS

  override fun createElement(node: ASTNode): PsiElement? {
    val type = node.getElementType()
    if (type is HCLElementType) {
      return Factory.createElement(node)
    }
    if (type is HCLTokenType) {
      return Factory.createElement(node)
    }
    return ASTWrapperPsiElement(node)
  }

  override fun createFile(fileViewProvider: FileViewProvider): PsiFile {
    return HCLFileImpl(fileViewProvider)
  }

  override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode) = ParserDefinition.SpaceRequirements.MAY

  companion object {
    public val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    public val STRING_LITERALS: TokenSet = TokenSet.create(SINGLE_QUOTED_STRING, DOUBLE_QUOTED_STRING)

    public val FILE: IFileElementType = IFileElementType(HCLLanguage)

    public val HCL_BRACES: TokenSet = TokenSet.create(L_CURLY, R_CURLY)
    public val HCL_BRACKETS: TokenSet = TokenSet.create(L_BRACKET, R_BRACKET)
    public val HCL_CONTAINERS: TokenSet = TokenSet.create(OBJECT, ARRAY)
    public val HCL_BOOLEANS: TokenSet = TokenSet.create(TRUE, FALSE)
    public val HCL_KEYWORDS: TokenSet = TokenSet.create(TRUE, FALSE, NULL)
    public val HCL_LITERALS: TokenSet = TokenSet.create(STRING_LITERAL, NUMBER_LITERAL, NULL_LITERAL, TRUE, FALSE)
    public val HCL_VALUES: TokenSet = TokenSet.orSet(HCL_CONTAINERS, HCL_LITERALS)
    public val HCL_COMMENTARIES: TokenSet = TokenSet.create(BLOCK_COMMENT, LINE_COMMENT)
  }
}
