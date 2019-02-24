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
package org.intellij.plugins.hcl.psi.impl;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.plugins.hcl.psi.HCLHeredocContent;
import org.intellij.plugins.hcl.psi.HCLStringLiteral;
import org.intellij.plugins.hcl.psi.UtilKt;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class JavaUtil {
  private static final Key<List<Pair<TextRange, String>>> STRING_FRAGMENTS = new Key<>("HCL string fragments");
  public static final String ourEscapesTable = "\"\"\\\\//b\bf\fn\nr\rt\tv\013a\007";
  public static final String ourEscapedSymbols = "\"\\/\b\f\n\r\t\013\007";
  @SuppressWarnings("SpellCheckingInspection")
  public static final String ourUnescapedSymbols = "\"\\/bfnrtva";

  @NotNull
  public static List<Pair<TextRange, String>> getTextFragments(@NotNull HCLStringLiteral literal) {
    List<Pair<TextRange, String>> result = literal.getFirstChild().getUserData(STRING_FRAGMENTS);
    if (result != null) return result;
    result = doGetTextFragments(literal.getText(), UtilKt.isInHCLFileWithInterpolations(literal), true);

    literal.getFirstChild().putUserData(STRING_FRAGMENTS, result);
    return result;
  }

  @NotNull
  public static List<Pair<TextRange, String>> getTextFragments(@NotNull final HCLHeredocContent content) {
    return CachedValuesManager.getCachedValue(content, () -> {
      List<Pair<TextRange, String>> result = doGetTextFragments(content.getText(), UtilKt.isInHCLFileWithInterpolations(content), false);
      return CachedValueProvider.Result.create(result, content);
    });
  }

  @NotNull
  static List<Pair<TextRange, String>> doGetTextFragments(@NotNull String text, boolean interpolations, boolean quotes) {
    List<Pair<TextRange, String>> result = ContainerUtil.newSmartList();
    final int length = text.length();
    int pos = quotes ? 1 : 0, unescapedSequenceStart = pos;
    int braces = 0;
    while (pos < length) {

      final char c = text.charAt(pos);
      if (interpolations && c == '$' && pos + 1 < length && text.charAt(pos + 1) == '{' && (pos == 0 || text.charAt(pos - 1) != '$')) {
        if (unescapedSequenceStart != pos) {
          result.add(Pair.create(new TextRange(unescapedSequenceStart, pos), text.substring(unescapedSequenceStart, pos)));
        }
        unescapedSequenceStart = pos;
        pos += 2;
        braces++;
        boolean inString = false;
        while (pos < length && braces > 0) {
          final char c2 = text.charAt(pos);
          if (!inString && c2 == '{') {
            braces++;
          } else if (!inString && c2 == '}') {
            braces--;
          } else if (c2 == '"' && (pos == 0 || text.charAt(pos-1) != '\\')) {
            inString = !inString;
          }
          pos++;
        }
        result.add(Pair.create(new TextRange(unescapedSequenceStart, pos), text.substring(unescapedSequenceStart, pos)));
        unescapedSequenceStart = pos;
        continue;
      }

      if (c == '\\') {
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
          case 'a':
          case 'b':
          case 'f':
          case 'n':
          case 'v':
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
          case 'U':
            i = pos + 2;
            for (; i < pos + 10; i++) {
              if (i == length || !StringUtil.isHexDigit(text.charAt(i))) {
                break;
              }
            }
            result.add(Pair.create(new TextRange(pos, i), text.substring(pos, i)));
            pos = i;
            break;
          case 'X':
            i = pos + 2;
            for (; i < pos + 4; i++) {
              if (i == length || !StringUtil.isHexDigit(text.charAt(i))) {
                break;
              }
            }
            result.add(Pair.create(new TextRange(pos, i), text.substring(pos, i)));
            pos = i;
            break;
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
            i = pos + 1;
            for (; i < pos + 4; i++) {
              if (i == length || !StringUtil.isOctalDigit(text.charAt(i))) {
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
    final int contentEnd = quotes && text.charAt(0) == text.charAt(length - 1) ? length - 1 : length;
    if (unescapedSequenceStart < contentEnd) {
      result.add(Pair.create(new TextRange(unescapedSequenceStart, contentEnd), text.substring(unescapedSequenceStart, contentEnd)));
    }
    result = Collections.unmodifiableList(result);
    return result;
  }

}
