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

import com.intellij.lexer.FlexAdapter
import java.util.EnumSet

public class HCLLexer(val capabilities: EnumSet<HCLCapability> = EnumSet.noneOf(javaClass<HCLCapability>())) : FlexAdapter(_HCLLexer(capabilities)) {

  companion object {
    private val STRING_START_MASK: Int = 0xFFFF shl 0x10 // 0xFFFF0000
    private val IN_SINGLE_QUOTED_STRING = 1 shl 15
    private val IN_STRING = 1 shl 14
    private val TIL_MASK = 0x00003F00 // 8-13

    private val JFLEX_STATE_MASK: Int = 0xFF
  }

  override fun getFlex(): _HCLLexer {
    return super.getFlex() as _HCLLexer
  }

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, state: Int) {
    val lexer = getFlex()
    if (capabilities.contains(HCLCapability.INTERPOLATION_LANGUAGE)) {
      if (state and IN_STRING == 0) {
        lexer.stringType = _HCLLexer.StringType.None
        lexer.stringStart = -1;
        lexer.til = 0;
      } else {
        lexer.stringType = if (state and IN_SINGLE_QUOTED_STRING == 0) _HCLLexer.StringType.DoubleQ else _HCLLexer.StringType.SingleQ
        lexer.stringStart = (state and STRING_START_MASK) ushr 0x10;
        lexer.til = (state and TIL_MASK) ushr 8;
      }
    }
    super.start(buffer, startOffset, endOffset, state and JFLEX_STATE_MASK)
  }

  override fun getState(): Int {
    val lexer = getFlex()
    var state = super.getState()
    if (capabilities.contains(HCLCapability.INTERPOLATION_LANGUAGE)) {
      val type = lexer.stringType!!
      if (type != _HCLLexer.StringType.None) {
        state = state or IN_STRING;
        if (type == _HCLLexer.StringType.SingleQ) {
          state = state or IN_SINGLE_QUOTED_STRING;
        }
      }
      state = state or (((lexer.til and 0x3f) shl 8) and TIL_MASK)
    }
    return state
  }
}
