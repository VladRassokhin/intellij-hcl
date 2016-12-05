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
package org.intellij.plugins.hil

import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.Lexer
import com.intellij.lexer.StringLiteralLexer
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
import org.intellij.plugins.hcl.HCLSyntaxHighlighterFactory
import org.intellij.plugins.hil.HILElementTypes.*
import org.intellij.plugins.hil.psi.HILLexer
import java.util.*

class HILSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

  companion object {
    val TIL_PARENS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.PARENS", DefaultLanguageHighlighterColors.PARENTHESES)
    val TIL_BRACES: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.BRACES", DefaultLanguageHighlighterColors.BRACES)
    val TIL_BRACKETS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val TIL_COMMA: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.COMMA", DefaultLanguageHighlighterColors.COMMA)
    val TIL_DOT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.DOT", DefaultLanguageHighlighterColors.DOT)
    val TIL_OPERATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val TIL_NUMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val TIL_STRING: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.STRING", DefaultLanguageHighlighterColors.STRING)
    val TIL_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    // Artificial element type
    val TIL_IDENTIFIER: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)

    // Added by annotators
    val TIL_PREDEFINED_SCOPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.PREDEFINED_SCOPE", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
    val TIL_RESOURCE_TYPE_REFERENCE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.RESOURCE_TYPE_REFERENCE", HCLSyntaxHighlighterFactory.HCL_BLOCK_SECOND_TYPE_KEY)
    val TIL_RESOURCE_INSTANCE_REFERENCE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.RESOURCE_INSTANCE_REFERENCE", HCLSyntaxHighlighterFactory.HCL_BLOCK_NAME_KEY)
    val TIL_PROPERTY_REFERENCE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.PROPERTY_REFERENCE", HCLSyntaxHighlighterFactory.HCL_PROPERTY_KEY)

    // String escapes
    val TIL_VALID_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.VALID_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
    val TIL_INVALID_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("TIL.INVALID_ESCAPE", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
  }

  object HILSyntaxHighlighter : SyntaxHighlighterBase() {
    val ourAttributes: Map<IElementType, TextAttributesKey> = HashMap()

    init {
      SyntaxHighlighterBase.fillMap(ourAttributes, HILParserDefinition.TIL_BRACES, TIL_BRACES)
      SyntaxHighlighterBase.fillMap(ourAttributes, HILParserDefinition.TIL_BRACKETS, TIL_BRACKETS)
      SyntaxHighlighterBase.fillMap(ourAttributes, HILParserDefinition.TIL_PARENS, TIL_PARENS)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_COMMA, COMMA)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_DOT, OP_DOT)
      SyntaxHighlighterBase.fillMap(ourAttributes, HILTokenTypes.IL_ALL_OPERATORS, TIL_OPERATOR)
      SyntaxHighlighterBase.fillMap(ourAttributes, HILParserDefinition.STRING_LITERALS, TIL_STRING)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_NUMBER, NUMBER)
      SyntaxHighlighterBase.fillMap(ourAttributes, HILParserDefinition.TIL_KEYWORDS, TIL_KEYWORD)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_IDENTIFIER, ID)
      SyntaxHighlighterBase.fillMap(ourAttributes, HighlighterColors.BAD_CHARACTER, TokenType.BAD_CHARACTER)

      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_VALID_ESCAPE, StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_INVALID_ESCAPE, StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN)
      SyntaxHighlighterBase.fillMap(ourAttributes, TIL_INVALID_ESCAPE, StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN)
    }


    override fun getTokenHighlights(tokenType: IElementType?): Array<out TextAttributesKey> {
      return SyntaxHighlighterBase.pack(ourAttributes[tokenType])
    }

    override fun getHighlightingLexer(): Lexer {
      val layeredLexer = LayeredLexer(HILLexer())
      layeredLexer.registerSelfStoppingLayer(StringLiteralLexer('\"', DOUBLE_QUOTED_STRING, false, "/", false, false), arrayOf(DOUBLE_QUOTED_STRING), IElementType.EMPTY_ARRAY)
      return layeredLexer
    }
  }

  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
    return HILSyntaxHighlighter
  }
}
