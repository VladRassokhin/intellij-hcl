package org.intellij.plugins.hcl.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.intellij.plugins.hcl.HCLFileType

/**
 * @author Mikhail Golubev
 */
public class HCLElementGenerator(private val myProject: Project) {

  /**
   * Create lightweight in-memory [org.intellij.plugins.hcl.psi.HCLFile] filled with `content`.

   * @param content content of the file to be created
   * *
   * @return created file
   */
  public fun createDummyFile(content: String): PsiFile {
    val psiFileFactory = PsiFileFactory.getInstance(myProject)
    return psiFileFactory.createFileFromText("dummy." + HCLFileType.getDefaultExtension(), HCLFileType, content)
  }

  /**
   * Create JSON value from supplied content.

   * @param content properly escaped text of JSON value, e.g. Java literal `&quot;\&quot;new\\nline\&quot;&quot;` if you want to create string literal
   * *
   * @param type of the JSON value desired
   * *
   * @return element created from given text
   * *
   * *
   * @see .createStringLiteral
   */
  public fun <T : HCLValue> createValue(content: String): T {
    val file = createDummyFile("{\"foo\": " + content + "}")
    //noinspection unchecked,ConstantConditions
    val property = (file.getFirstChild() as HCLObject).getPropertyList().get(0)
    return (property as HCLProperty).getValue() as T
  }

  public fun createObject(content: String): HCLObject {
    val file = createDummyFile("{" + content + "}")
    //noinspection unchecked,ConstantConditions
    return file.getFirstChild() as HCLObject
  }

  /**
   * Create JSON string literal from supplied *unescaped* content.

   * @param unescapedContent unescaped content of string literal, e.g. Java literal `&quot;new\nline&quot;` (compare with [.createValue]).
   * *
   * @return JSON string literal created from given text
   */
  public fun createStringLiteral(unescapedContent: String): HCLStringLiteral {
    return createValue('"' + StringUtil.escapeStringCharacters(unescapedContent) + '"')
  }

  public fun createProperty(name: String, value: String): HCLProperty {
    val file = createDummyFile("{\"" + name + "\": " + value + "}")
    //noinspection unchecked,ConstantConditions
    return (file.getFirstChild() as HCLObject).getPropertyList().get(0)
  }

  public fun createComma(): PsiElement {
    val jsonArray1 = createValue<HCLArray>("[1, 2]")
    return jsonArray1.getValueList().get(0).getNextSibling()
  }
}
