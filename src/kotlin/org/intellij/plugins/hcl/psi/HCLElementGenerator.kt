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
package org.intellij.plugins.hcl.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.intellij.plugins.hcl.HCLFileType

/**
 * @author Mikhail Golubev
 * @author Vladislav Rassokhin
 */
public open class HCLElementGenerator(private val project: Project) {

  /**
   * Create lightweight in-memory [org.intellij.plugins.hcl.psi.HCLFile] filled with `content`.

   * @param content content of the file to be created
   * *
   * @return created file
   */
  public open fun createDummyFile(content: String): PsiFile {
    val psiFileFactory = PsiFileFactory.getInstance(project)
    return psiFileFactory.createFileFromText("dummy." + HCLFileType.defaultExtension, HCLFileType, content)
  }

  /**
   * Create HCL value from supplied content.

   * @param content properly escaped text of HCL value, e.g. Java literal `&quot;\&quot;new\\nline\&quot;&quot;` if you want to create string literal
   * *
   * @param type of the HCL value desired
   * *
   * @return element created from given text
   * *
   * *
   * @see .createStringLiteral
   */
  public fun <T : HCLValue> createValue(content: String): T {
    val property = createProperty("foo", content)
    @Suppress("UNCHECKED_CAST")
    return property.value as T
  }

  public fun createObject(content: String): HCLObject {
    val file = createDummyFile("foo {$content}")
    val block = file.firstChild as HCLBlock
    return block.`object`!!
  }

  /**
   * Create HCL string literal from supplied *unescaped* content.

   * @param unescapedContent unescaped content of string literal, e.g. Java literal `&quot;new\nline&quot;` (compare with [.createValue]).
   * *
   * @return HCL string literal created from given text
   */
  public fun createStringLiteral(unescapedContent: String): HCLStringLiteral {
    return createValue("\"${StringUtil.escapeStringCharacters(unescapedContent)}\"")
  }

  public fun createProperty(name: String, value: String): HCLProperty {
    val s: String
    if (isIdentifier(name)) {
      s = "$name = $value"
    } else {
      s = "\"$name\" = $value"
    }
    val file = createDummyFile(s)
    return file.firstChild as HCLProperty
  }

  private fun isIdentifier(name: String): Boolean {
    return name.matches("\\w*".toRegex())
  }

  public fun createBlock(name: String): HCLBlock {
    val file = createDummyFile("\"$name\" {}")
    return file.firstChild as HCLBlock
  }

  public fun createComma(): PsiElement {
    val array = createValue<HCLArray>("[1, 2]")
    return array.valueList[0].nextSibling
  }

  public fun createIdentifier(name: String): HCLIdentifier {
    val file = createDummyFile("$name=true")
    val property = file.firstChild as HCLProperty
    return property.nameElement as HCLIdentifier
  }

  public fun createHeredocContent(lines: List<String>): HCLHeredocContent {
    var text = buildString {
      append("x=<<___EOF___\n")
      for (l in lines) {
        append(l.removeSuffix("\n"))
        append('\n')
      }
      append("___EOF___")
    }
    val file = createDummyFile(text)
    val property = file.firstChild as HCLProperty
    return (property.value as HCLHeredocLiteral).content
  }
}
