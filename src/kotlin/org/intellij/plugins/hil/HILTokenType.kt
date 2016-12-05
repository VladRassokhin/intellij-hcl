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

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.intellij.plugins.hil.HILElementTypes.*


open class HILTokenType(debugName: String) : IElementType(debugName, HILLanguage)

object HILTokenTypes {
  init {
    assert(OP_PLUS != null)
  }
  val IL_BINARY_OPERATORS: TokenSet = TokenSet.create(
      OP_PLUS, OP_MINUS, OP_MUL, OP_DIV, OP_MOD,
      OP_EQUAL, OP_NOT_EQUAL,
      OP_LESS, OP_GREATER, OP_LESS_OR_EQUAL, OP_GREATER_OR_EQUAL,
      OP_AND_AND, OP_OR_OR
  )

  val IL_UNARY_OPERATORS: TokenSet = TokenSet.create(
      OP_PLUS, OP_MINUS, OP_NOT
  )

  val IL_TERNARY_OPERATOR_TOKENS = TokenSet.create(
      OP_QUEST, OP_COLON
  )

  val IL_ALL_OPERATORS = TokenSet.orSet(IL_UNARY_OPERATORS, IL_BINARY_OPERATORS, IL_TERNARY_OPERATOR_TOKENS)
}