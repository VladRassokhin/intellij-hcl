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
package org.intellij.plugins.hcl.psi.impl;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.plugins.hcl.psi.HCLStringLiteral;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaUtil {
  private static final Key<List<Pair<TextRange, String>>> STRING_FRAGMENTS = new Key<List<Pair<TextRange, String>>>("HCL string fragments");
  public static final String ourEscapesTable = "\"\"\\\\//b\bf\fn\nr\rt\t";

  @NotNull
  public static List<Pair<TextRange, String>> getTextFragments(@NotNull HCLStringLiteral literal) {
    List<Pair<TextRange, String>> result = literal.getUserData(STRING_FRAGMENTS);
    if (result == null) {
      result = new ArrayList<Pair<TextRange, String>>();
      final String text = literal.getText();
      final int length = text.length();
      int pos = 1, unescapedSequenceStart = 1;
      while (pos < length) {
        if (text.charAt(pos) == '\\') {
          if (unescapedSequenceStart != pos) {
            result.add(Pair.create(new TextRange(unescapedSequenceStart, pos), text.substring(unescapedSequenceStart, pos)));
          }
          if (pos == length - 1) {
            result.add(Pair.create(new TextRange(pos, pos + 1), "\\"));
            break;
          }
          final char next = text.charAt(pos + 1);
          switch (next) {
            case '"':
            case '\\':
            case '/':
            case 'b':
            case 'f':
            case 'n':
            case 'r':
            case 't':
              final int idx = ourEscapesTable.indexOf(next);
              result.add(Pair.create(new TextRange(pos, pos + 2), ourEscapesTable.substring(idx + 1, idx + 2)));
              pos += 2;
              break;
            case 'u':
              int i = pos + 2;
              for (; i < pos + 6; i++) {
                if (i == length || !StringUtil.isHexDigit(text.charAt(i))) {
                  break;
                }
              }
              result.add(Pair.create(new TextRange(pos, i), text.substring(pos, i)));
              pos = i;
              break;
            default:
              result.add(Pair.create(new TextRange(pos, pos + 2), text.substring(pos, pos + 2)));
              pos += 2;
          }
          unescapedSequenceStart = pos;
        } else {
          pos++;
        }
      }
      final int contentEnd = text.charAt(0) == text.charAt(length - 1) ? length - 1 : length;
      if (unescapedSequenceStart < contentEnd) {
        result.add(Pair.create(new TextRange(unescapedSequenceStart, length), text.substring(unescapedSequenceStart, contentEnd)));
      }
      result = Collections.unmodifiableList(result);
      literal.putUserData(STRING_FRAGMENTS, result);
    }
    return result;
  }

}
