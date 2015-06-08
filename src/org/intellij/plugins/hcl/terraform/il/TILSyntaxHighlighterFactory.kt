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

import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.intellij.plugins.hcl.terraform.il.TILElementTypes.*
import org.intellij.plugins.hcl.terraform.il.psi.TILLexer
import java.util.HashMap

public class TILSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

  object TILSyntaxHighlighter : SyntaxHighlighterBase() {
    val ourAttributes: Map<IElementType, TextAttributesKey> = HashMap()

    public val TIL_PARENS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.PARENS", DefaultLanguageHighlighterColors.PARENTHESES)
    public val TIL_BRACES: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.BRACES", DefaultLanguageHighlighterColors.BRACES)
    public val TIL_COMMA: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.COMMA", DefaultLanguageHighlighterColors.COMMA)
    public val TIL_EQUALS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.EQUALS", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    public val TIL_OPERATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    public val TIL_NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    public val TIL_STRING: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.STRING", DefaultLanguageHighlighterColors.STRING)
    public val TIL_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    // Artificial element type
    public val TIL_IDENTIFIER: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)

    // String escapes
    public val TIL_VALID_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.VALID_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
    public val TIL_INVALID_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.INVALID_ESCAPE", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)


    init {
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_BRACES, INTERPOLATION_START, L_CURLY, R_CURLY)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_PARENS, L_PAREN, R_PAREN)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_COMMA, COMMA)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_EQUALS, EQUALS)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_OPERATOR, OP_PLUS, OP_MINUS, OP_MUL, OP_DIV, OP_MOD)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_STRING, DOUBLE_QUOTED_STRING)
      //      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_STRING, SINGLE_QUOTED_STRING)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_NUMBER, NUMBER)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_KEYWORD, TRUE, FALSE, NULL)
      // TODO may be it's worth to add more sensible highlighting for identifiers
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_IDENTIFIER, ID)
      SyntaxHighlighterBase.fillMap(ourAttributes, HighlighterColors.BAD_CHARACTER, TokenType.BAD_CHARACTER)

      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_VALID_ESCAPE, StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_INVALID_ESCAPE, StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_INVALID_ESCAPE, StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN)
    }


    override fun getTokenHighlights(tokenType: IElementType?): Array<out TextAttributesKey> {
      return SyntaxHighlighterBase.pack(ourAttributes.get(tokenType))
    }

    override fun getHighlightingLexer(): Lexer {
      val layeredLexer = LayeredLexer(TILLexer())

      //      layeredLexer.registerSelfStoppingLayer(StringLiteralLexer('\"', DOUBLE_QUOTED_STRING, false, "/", false, false), arrayOf(DOUBLE_QUOTED_STRING), IElementType.EMPTY_ARRAY)
      //      layeredLexer.registerSelfStoppingLayer(StringLiteralLexer('\'', SINGLE_QUOTED_STRING, false, "/", false, false), arrayOf(SINGLE_QUOTED_STRING), IElementType.EMPTY_ARRAY)
      return layeredLexer
    }
  }

  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
    return TILSyntaxHighlighter
  }
}
