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
package org.intellij.plugins.hcl.terraform.il

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.plugins.hcl.terraform.il.psi.ILPsiFile
import org.intellij.plugins.hcl.terraform.il.psi.TILLexer
import org.intellij.plugins.hcl.terraform.il.psi.impl.ILExpressionHolderImpl

public class TILParserDefinition : ParserDefinition {

  override fun createLexer(project: Project) = TILLexer()

  override fun createParser(project: Project) = TILParser()

  override fun getFileNodeType() = FILE

  override fun getWhitespaceTokens() = WHITE_SPACES

  override fun getCommentTokens(): TokenSet {
    return TokenSet.EMPTY
  }

  override fun getStringLiteralElements() = STRING_LITERALS

  override fun createElement(node: ASTNode): PsiElement? {
    val type = node.getElementType()
    if (type == IL_HOLDER) {
      return ILExpressionHolderImpl(node)
    }
    if (type is TILElementType) {
      return TILElementTypes.Factory.createElement(node)
    }
    if (type is TILTokenType) {
      return TILElementTypes.Factory.createElement(node)
    }
    return ASTWrapperPsiElement(node)
  }

  override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return ILPsiFile(viewProvider)
  }

  override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode) = ParserDefinition.SpaceRequirements.MAY

  companion object {
    public val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    public val STRING_LITERALS: TokenSet = TokenSet.create(TILElementTypes.DOUBLE_QUOTED_STRING)

    public val FILE: IFileElementType = IFileElementType(TILLanguage)

    public val TIL_BRACES: TokenSet = TokenSet.create(TILElementTypes.L_CURLY, TILElementTypes.R_CURLY)
    public val TIL_PARENS: TokenSet = TokenSet.create(TILElementTypes.L_PAREN, TILElementTypes.R_PAREN)
    public val TIL_BOOLEANS: TokenSet = TokenSet.create(TILElementTypes.TRUE, TILElementTypes.FALSE)
    public val TIL_KEYWORDS: TokenSet = TokenSet.create(TILElementTypes.TRUE, TILElementTypes.FALSE, TILElementTypes.NULL)
    public val TIL_LITERALS: TokenSet = TokenSet.create(TILElementTypes.IL_LITERAL_EXPRESSION, TILElementTypes.TRUE, TILElementTypes.FALSE)
    public val TIL_VALUES: TokenSet = TokenSet.orSet(TIL_LITERALS)

    private val ourContextNodeKey: Key<ASTNode> = Key.create("Terraform-IL.context.node");

    public val IL_HOLDER: ILazyParseableElementType = object : ILazyParseableElementType("IL_HOLDER", TILLanguage) {
      override fun parseContents(chameleon: ASTNode?): ASTNode? {
        chameleon!!
        val psi = chameleon.getPsi()
        assert (psi != null, chameleon)
        psi!!
        val project = psi.getProject()
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon)
        val parser = LanguageParserDefinitions.INSTANCE.forLanguage(getLanguage()).createParser(project)

        builder.putUserData(ourContextNodeKey, chameleon.getTreeParent())
        val node = parser.parse(this, builder).getFirstChildNode()
        builder.putUserData(ourContextNodeKey, null)
        return node;
      }
    }
  }
}
