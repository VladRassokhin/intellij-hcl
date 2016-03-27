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
package org.intellij.plugins.hcl;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

class LexerUtil {
  /**
   * Copy of StringUtil#trimLeading(CharSequence) which is not present in 139 branch.
   * TODO: Remove once minimum platform version would be 141.
   */
  @NotNull
  @Contract(pure = true)
  static CharSequence trimLeading(@NotNull CharSequence string) {
    int index = 0;
    while (index < string.length() && Character.isWhitespace(string.charAt(index))) index++;
    return string.subSequence(index, string.length());
  }
}
