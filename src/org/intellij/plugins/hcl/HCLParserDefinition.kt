package org.intellij.plugins.hcl

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
import org.intellij.plugins.hcl.psi.HCLLexer
import org.intellij.plugins.hcl.psi.impl.HCLFileImpl

import org.intellij.plugins.hcl.HCLElementTypes.*

public class HCLParserDefinition : ParserDefinition {

  override fun createLexer(project: Project) = HCLLexer()

  override fun createParser(project: Project) = HCLParser()

  override fun getFileNodeType() = FILE

  override fun getWhitespaceTokens() = WHITE_SPACES

  override fun getCommentTokens() = HCL_COMMENTARIES

  override fun getStringLiteralElements() = STRING_LITERALS

  override fun createElement(node: ASTNode) = Factory.createElement(node)

  override fun createFile(fileViewProvider: FileViewProvider) = HCLFileImpl(fileViewProvider)

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
