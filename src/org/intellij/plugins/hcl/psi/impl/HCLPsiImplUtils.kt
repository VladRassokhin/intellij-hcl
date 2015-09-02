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
package org.intellij.plugins.hcl.psi.impl

import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.SmartList
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.psi.*
import javax.swing.Icon

public object HCLPsiImplUtils {
  public fun getName(property: HCLProperty): String {
    return StringUtil.unescapeStringCharacters(HCLPsiUtil.stripQuotes(property.getNameElement().getText()))
  }

  public fun getName(block: HCLBlock): String {
    val elements = block.getNameElements()
    val sb = StringBuilder()
    for (element in elements) {
      sb.append(StringUtil.unescapeStringCharacters(HCLPsiUtil.stripQuotes(element.getText()))).append(' ')
    }
    return sb.toString().trim()
  }

  public fun getName(marker: HCLHeredocMarker): String {
    return marker.getFirstChild().getText()
  }

  /**
   * Actually only JSON string literal should be accepted as valid name of property according to standard,
   * but for compatibility with JavaScript integration any JSON literals as well as identifiers (unquoted words)
   * are possible and highlighted as error later.

   * @see HCLStandardComplianceInspection
   */
  public fun getNameElement(property: HCLProperty): HCLValue {
    val firstChild = property.getFirstChild()
    assert(firstChild is HCLLiteral || firstChild is HCLIdentifier, "Excepted literal or identifier, got ${firstChild.javaClass.getName()}")
    return firstChild as HCLValue
  }

  public fun getNameElements(block: HCLBlock): Array<HCLElement> {
    var result: MutableList<HCLElement>? = null
    var child: PsiElement? = block.getFirstChild()
    while (child != null) {
      if (child is HCLIdentifier || child is HCLStringLiteral) {
        if (result == null) result = SmartList<HCLElement>()
        //noinspection unchecked
        result.add(child as HCLElement)
      }
      child = child.getNextSibling()
    }
    return if (result == null) emptyArray<HCLElement>() else ArrayUtil.toObjectArray<HCLElement>(result, HCLElement::class.java)
  }

  public fun getValue(property: HCLProperty): HCLValue? {
    return PsiTreeUtil.getNextSiblingOfType<HCLValue>(getNameElement(property), HCLValue::class.java)
  }

  public fun getObject(block: HCLBlock): HCLObject? {
    return PsiTreeUtil.getNextSiblingOfType<HCLObject>(block.getFirstChild(), HCLObject::class.java)
  }

  public fun isQuotedString(literal: HCLLiteral): Boolean {
    return literal.getNode().findChildByType(HCLParserDefinition.STRING_LITERALS) != null
  }

  public fun getPresentation(property: HCLProperty): ItemPresentation? {
    return object : ItemPresentation {
      override fun getPresentableText(): String? {
        return property.getName()
      }

      override fun getLocationString(): String? {
        return null
      }

      override fun getIcon(unused: Boolean): Icon? {
        if (property.getValue() is HCLArray) {
          return IconLoader.getIcon("/hcl/property_brackets.png")
        }
        if (property.getValue() is HCLObject) {
          return IconLoader.getIcon("/hcl/property_braces.png")
        }
        return PlatformIcons.PROPERTY_ICON
      }
    }
  }

  public fun getPresentation(block: HCLBlock): ItemPresentation? {
    return object : ItemPresentation {
      override fun getPresentableText(): String? {
        return block.getName()
      }

      override fun getLocationString(): String? {
        return null
      }

      override fun getIcon(unused: Boolean): Icon? {
        if (block.getObject() is HCLArray) {
          return IconLoader.getIcon("/hcl/property_brackets.png")
        }
        if (block.getObject() is HCLObject) {
          return IconLoader.getIcon("/hcl/property_braces.png")
        }
        return PlatformIcons.PROPERTY_ICON
      }
    }
  }

  public fun getPresentation(array: HCLArray): ItemPresentation? {
    return object : ItemPresentation {
      override fun getPresentableText(): String? {
        return ("hcl.array")
      }

      override fun getLocationString(): String? {
        return null
      }

      override fun getIcon(unused: Boolean): Icon? {
        return AllIcons.Json.Array
      }
    }
  }

  public fun getPresentation(o: HCLObject): ItemPresentation? {
    return object : ItemPresentation {
      override fun getPresentableText(): String? {
        return ("hcl.object")
      }

      override fun getLocationString(): String? {
        return null
      }

      override fun getIcon(unused: Boolean): Icon? {
        return AllIcons.Json.Object
      }
    }
  }

  public fun getTextFragments(literal: HCLStringLiteral): List<Pair<TextRange, String>> = JavaUtil.getTextFragments(literal)

  //  public static void delete(@NotNull HCLProperty property) {
  //    final ASTNode myNode = property.getNode();
  //    HCLPsiChangeUtils.removeCommaSeparatedFromList(myNode, myNode.getTreeParent());
  //  }

  public fun findProperty(`object`: HCLObject, name: String): HCLProperty? {
    val properties = PsiTreeUtil.findChildrenOfType<HCLProperty>(`object`, HCLProperty::class.java)
    for (property in properties) {
      if (property.getName() == name) {
        return property
      }
    }
    return null
  }

  public fun getValue(literal: HCLStringLiteral): String {
    return StringUtil.unescapeStringCharacters(HCLPsiUtil.stripQuotes(literal.getText()))
  }

  public fun getValue(literal: HCLHeredocLiteral): String {
    val builder = StringBuilder()
    literal.getLinesList().forEach {builder.append(it.getText())}
    return builder.toString()
  }

  public fun getValue(line: HCLHeredocLine): String {
    return line.getText()
  }

  public fun getValue(literal: HCLBooleanLiteral): Boolean {
    return literal.textMatches("true")
  }

  public fun getValue(literal: HCLNumberLiteral): Double {
    val text = literal.getText()
    val index = text.indexOfAny("KMGB".toCharArray(), 0, true)
    if (index != -1) {
      val base = java.lang.Double.parseDouble(text.substring(0, index));
      val suffixValue = getSuffixValue(text.substring(index));
      return base * suffixValue;
    } else {
      return java.lang.Double.parseDouble(text)
    }
  }

  private fun getSuffixValue(suffix: String): Long {
    val base: Int
    when(suffix.length()) {
      0 -> return 1;
      1 -> {
        base = 1000;
      }
      2 -> {
        assert(suffix.get(1).toLowerCase().equals('b'))
        base = 1024;
      }
      else -> throw IllegalArgumentException("Unsupported suffix '${suffix}'")
    }
    when (suffix.get(0).toLowerCase()) {
      'b' -> return 1;
      'k' -> return pow(base, 1);
      'm' -> return pow(base, 2);
      'g' -> return pow(base, 3);
      else -> throw IllegalArgumentException("Unsupported suffix '${suffix}'")
    }
  }

  private fun pow(a: Int, b: Int): Long {
    if (b == 0) return 1;
    var result = a.toLong()
    for (i in 1.rangeTo(b)) {
      result *= result
    }
    return result;
  }


  public fun getId(identifier: HCLIdentifier): String {
    return identifier.getText()
  }
}
