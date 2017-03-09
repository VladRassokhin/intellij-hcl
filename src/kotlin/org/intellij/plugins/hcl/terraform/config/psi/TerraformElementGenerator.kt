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
package org.intellij.plugins.hcl.terraform.config.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElementGenerator
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.intellij.plugins.hcl.terraform.config.model.Type
import org.intellij.plugins.hcl.terraform.config.model.Types
import org.intellij.plugins.hil.psi.ILExpression

class TerraformElementGenerator(val project: Project) : HCLElementGenerator(project) {
  override fun createDummyFile(content: String): PsiFile {
    val psiFileFactory = PsiFileFactory.getInstance(project)
    return psiFileFactory.createFileFromText("dummy." + TerraformFileType.defaultExtension, TerraformFileType, content)
  }

  fun createVariable(name: String, type: Type?, initializer: ILExpression): HCLBlock {
    val value = initializer.text // TODO: Improve
    return createVariable(name, type, value)
  }

  fun createVariable(name: String, type: Type?, value: String): HCLBlock {
    val content = buildString {
      append("variable \"").append(name).append("\" {")
      val typeName = when(type) {
        Types.String -> "string"
        Types.Array -> "list"
        Types.Object -> "map"
        else -> null
      }
      if (typeName != null) {
        append("\n  type=\"").append(typeName).append("\"")
      }
      append("\n  default=").append(value).append("\n}")
    }
    val file = createDummyFile(content)
    return file.firstChild as HCLBlock
  }
}
