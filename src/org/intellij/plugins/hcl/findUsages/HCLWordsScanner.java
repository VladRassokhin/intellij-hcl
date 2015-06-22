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
package org.intellij.plugins.hcl.findUsages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;
import org.intellij.plugins.hcl.HCLElementTypes;
import org.intellij.plugins.hcl.HCLParserDefinition;
import org.intellij.plugins.hcl.psi.HCLLexer;

public class HCLWordsScanner extends DefaultWordsScanner {
  public HCLWordsScanner() {
    super(new HCLLexer(), TokenSet.create(HCLElementTypes.IDENTIFIER), HCLParserDefinition.HCL_COMMENTARIES, HCLParserDefinition.HCL_LITERALS);
    setMayHaveFileRefsInLiterals(true);
  }
}
