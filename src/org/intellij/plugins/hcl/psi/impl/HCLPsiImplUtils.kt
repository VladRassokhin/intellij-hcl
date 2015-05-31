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

  /**
   * Actually only JSON string literal should be accepted as valid name of property according to standard,
   * but for compatibility with JavaScript integration any JSON literals as well as identifiers (unquoted words)
   * are possible and highlighted as error later.

   * @see HCLStandardComplianceInspection
   */
  public fun getNameElement(property: HCLProperty): HCLValue {
    val firstChild = property.getFirstChild()
    assert(firstChild is HCLLiteral || firstChild is HCLIdentifier)
    return firstChild as HCLValue
  }

  public fun getNameElements(block: HCLBlock): Array<HCLElement> {
    var result: MutableList<HCLElement>? = null
    var child: PsiElement? = block.getFirstChild()
    while (child != null) {
      if ((child is HCLIdentifier || child is HCLLiteral)) {
        if (result == null) result = SmartList<HCLElement>()
        //noinspection unchecked
        result.add(child as HCLElement)
      }
      child = child.getNextSibling()
    }
    return if (result == null) emptyArray<HCLElement>() else ArrayUtil.toObjectArray<HCLElement>(result, javaClass<HCLElement>())
  }

  public fun getValue(property: HCLProperty): HCLValue? {
    return PsiTreeUtil.getNextSiblingOfType<HCLValue>(getNameElement(property), javaClass<HCLValue>())
  }

  public fun getObject(block: HCLBlock): HCLValue? {
    return PsiTreeUtil.getNextSiblingOfType<HCLObject>(block.getFirstChild(), javaClass<HCLObject>())
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
          return AllIcons.Json.Property_brackets
        }
        if (property.getValue() is HCLObject) {
          return AllIcons.Json.Property_braces
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
          return AllIcons.Json.Property_brackets
        }
        if (block.getObject() is HCLObject) {
          return AllIcons.Json.Property_braces
        }
        return PlatformIcons.PROPERTY_ICON
      }
    }
  }

  public fun getPresentation(array: HCLArray): ItemPresentation? {
    return object : ItemPresentation {
      override fun getPresentableText(): String? {
        return ("json.array")
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
        return ("json.object")
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
    val properties = PsiTreeUtil.findChildrenOfType<HCLProperty>(`object`, javaClass<HCLProperty>())
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

  public fun getValue(literal: HCLBooleanLiteral): Boolean {
    return literal.textMatches("true")
  }

  public fun getValue(literal: HCLNumberLiteral): Double {
    return java.lang.Double.parseDouble(literal.getText())
  }

  public fun getId(identifier: HCLIdentifier): String {
    return identifier.getText()
  }
}
