/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.psi;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.util.Key;

public class HCLParserUtil extends GeneratedParserUtilBase {
  public static final Key<Object> KEY = Key.create("inside conditional");

  public static boolean push(PsiBuilder builder_, int i, int i1) {
    builder_.putUserData(KEY, i1);
    return true;
  }

  public static boolean pop(PsiBuilder builder_, int i) {
    builder_.putUserData(KEY, null);
    return true;
  }

  public static boolean peek(PsiBuilder builder_, int i, int i1) {
    return builder_.getUserData(KEY) != null;
  }
}
