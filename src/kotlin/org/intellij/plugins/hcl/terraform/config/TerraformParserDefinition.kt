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
package org.intellij.plugins.hcl.terraform.config

import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import org.intellij.plugins.hcl.HCLCapability
import org.intellij.plugins.hcl.HCLLexer
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.impl.HCLFileImpl
import java.util.*

public open class TerraformParserDefinition : HCLParserDefinition() {
  companion object {
    public val LexerCapabilities: EnumSet<HCLCapability> = EnumSet.of(HCLCapability.INTERPOLATION_LANGUAGE, HCLCapability.NUMBERS_WITH_BYTES_POSTFIX);
    public val FILE: IFileElementType = IFileElementType(TerraformLanguage)
  }

  // TODO: Add special parser with psi elements in terms of Terraform (resource, provider, etc)

  override fun createLexer(project: Project): Lexer {
    return HCLLexer(LexerCapabilities);
  }

  override fun getFileNodeType(): IFileElementType {
    return FILE
  }

  override fun createFile(fileViewProvider: FileViewProvider): PsiFile {
    return HCLFileImpl(fileViewProvider, TerraformLanguage)
  }
}