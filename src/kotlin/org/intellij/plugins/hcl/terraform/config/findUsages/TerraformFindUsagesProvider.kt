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
package org.intellij.plugins.hcl.terraform.config.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import org.intellij.plugins.hcl.HCLLexer
import org.intellij.plugins.hcl.findUsages.HCLFindUsagesProvider
import org.intellij.plugins.hcl.findUsages.HCLWordsScanner
import org.intellij.plugins.hcl.terraform.config.TerraformParserDefinition

class TerraformFindUsagesProvider : HCLFindUsagesProvider() {
  override fun getWordsScanner(): WordsScanner? {
    return HCLWordsScanner(HCLLexer(TerraformParserDefinition.LexerCapabilities))
  }
}