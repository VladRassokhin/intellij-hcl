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
package org.intellij.plugins.hcl.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.SmartList
import org.intellij.plugins.hcl.HCLParserDefinition
import org.intellij.plugins.hcl.Icons
import org.intellij.plugins.hcl.psi.*
import javax.swing.Icon

val CharSequence.indentation: Int
  get() {
    var x = 0;
    forEach {
      if (it.isWhitespace()) x++
      else return x
    }
    return x
  }

object HCLPsiImplUtils {
  private val LOG = Logger.getInstance(HCLPsiImplUtils::class.java)

  fun getName(property: HCLProperty): String {
    return StringUtil.unescapeStringCharacters(HCLPsiUtil.stripQuotes(property.nameElement.text))
  }

  fun getName(block: HCLBlock): String {
    val identifier = block.nameIdentifier
    if (identifier != null) {
      return if (identifier is PsiNamedElement) identifier.name ?: identifier.text else identifier.text
    }
    return getFullName(block)
  }

  fun getFullName(block: HCLBlock): String {
    val elements = block.nameElements
    val sb = StringBuilder()
    for (element in elements) {
      sb.append(StringUtil.unescapeStringCharacters(HCLPsiUtil.stripQuotes(element.text))).append(' ')
    }
    return sb.toString().trim()
  }

  fun getName(marker: HCLHeredocMarker): String {
    return marker.firstChild.text
  }

  /**
   * Actually only JSON string literal should be accepted as valid name of property according to standard,
   * but for compatibility with JavaScript integration any JSON literals as well as identifiers (unquoted words)
   * are possible and highlighted as error later.

   * @see HCLStandardComplianceInspection
   */
  fun getNameElement(property: HCLProperty): HCLValue {
    val firstChild = property.firstChild
    assert(firstChild is HCLLiteral || firstChild is HCLIdentifier) { "Excepted literal or identifier, got ${firstChild.javaClass.name}" }
    return firstChild as HCLValue
  }

  fun getNameElements(block: HCLBlock): Array<HCLElement> {
    var result: MutableList<HCLElement>? = null
    var child: PsiElement? = block.firstChild
    while (child != null) {
      if (child is HCLIdentifier || child is HCLStringLiteral) {
        if (result == null) result = SmartList<HCLElement>()
        //noinspection unchecked
        result.add(child as HCLElement)
      }
      child = child.nextSibling
    }
    return if (result == null) emptyArray<HCLElement>() else ArrayUtil.toObjectArray<HCLElement>(result, HCLElement::class.java)
  }

  fun getValue(property: HCLProperty): HCLValue? {
    return PsiTreeUtil.getNextSiblingOfType<HCLValue>(getNameElement(property), HCLValue::class.java)
  }

  fun getObject(block: HCLBlock): HCLObject? {
    return PsiTreeUtil.getNextSiblingOfType<HCLObject>(block.firstChild, HCLObject::class.java)
  }

  fun isQuotedString(literal: HCLLiteral): Boolean {
    return literal.node.findChildByType(HCLParserDefinition.STRING_LITERALS) != null
  }

  open class AbstractItemPresentation(val element: HCLElement) : ItemPresentation {
    override fun getPresentableText(): String? {
      if (element is PsiNamedElement) {
        @Suppress("USELESS_CAST")
        return (element as PsiNamedElement).name
      }
      return null
    }

    override fun getLocationString(): String? {
      if (true) return null;
      if (element.parent is HCLFile) {
        return element.containingFile.containingDirectory.name
      } else if (element.parent is HCLObject) {
        val pp = element.parent.parent
        if (pp is HCLBlock) {
          return pp.fullName
        } else if (pp is PsiNamedElement) {
          return pp.name
        }
      }
      return null
    }

    override fun getIcon(p0: Boolean): Icon? {
      return null
    }
  }

  fun getPresentation(property: HCLProperty): ItemPresentation? {
    return object : AbstractItemPresentation(property) {
      override fun getIcon(unused: Boolean): Icon? {
        if (property.value is HCLArray) {
          return Icons.PropertyBrackets
        }
        if (property.value is HCLObject) {
          return Icons.PropertyBraces
        }
        return Icons.Property
      }
    }
  }

  fun getPresentation(block: HCLBlock): ItemPresentation? {
    return object : AbstractItemPresentation(block) {
      override fun getPresentableText(): String? {
        return block.fullName
      }

      override fun getIcon(unused: Boolean): Icon? {
        if (block.`object` is HCLArray) {
          return Icons.PropertyBrackets
        }
        if (block.`object` is HCLObject) {
          return Icons.PropertyBraces
        }
        return Icons.Property
      }
    }
  }

  fun getPresentation(array: HCLArray): ItemPresentation? {
    return object : AbstractItemPresentation(array) {
      override fun getPresentableText(): String? {
        return ("hcl.array")
      }

      override fun getIcon(unused: Boolean): Icon? {
        return Icons.Array
      }
    }
  }

  fun getPresentation(o: HCLObject): ItemPresentation? {
    return object : AbstractItemPresentation(o) {
      override fun getPresentableText(): String? {
        return ("hcl.object")
      }

      override fun getIcon(unused: Boolean): Icon? {
        return Icons.Object
      }
    }
  }

  fun getTextFragments(literal: HCLStringLiteral): List<Pair<TextRange, String>> = JavaUtil.getTextFragments(literal)

  //  public static void delete(@NotNull HCLProperty property) {
  //    final ASTNode myNode = property.getNode();
  //    HCLPsiChangeUtils.removeCommaSeparatedFromList(myNode, myNode.getTreeParent());
  //  }

  fun findProperty(`object`: HCLObject, name: String): HCLProperty? {
    for (property in `object`.propertyList) {
      if (property.name == name) {
        return property
      }
    }
    return null
  }

  fun getValue(literal: HCLStringLiteral): String {
    val interpolations = literal.isInHCLFileWithInterpolations()
    val text = literal.text
    val unquote: String?
    try {
      unquote = HCLQuoter.unquote(text, interpolations)
    } catch(e: Exception) {
      LOG.warn("Failed to unquote string: ${e.message}", e)
      unquote = null
    }
    if (unquote == null) {
      return HCLQuoter.unquote(text, interpolations, safe = true)!!
    }
    return unquote
  }

  fun getQuoteSymbol(literal: HCLStringLiteral): Char {
    return literal.text[0]
  }

  fun getValue(literal: HCLHeredocLiteral): String {
    return getValue(literal.content, literal.indentation ?: 0)
  }

  fun isIndented(literal: HCLHeredocLiteral): Boolean {
    return literal.markerStart.text.startsWith('-')
  }

  fun getIndentation(literal: HCLHeredocLiteral): Int? {
    if (!isIndented(literal)) return null
    val markerEnd = literal.markerEnd ?: return null
    var indentation = markerEnd.text.indentation
    val contentMinIndentation = literal.content.minimalIndentation ?: return indentation
    if (contentMinIndentation < indentation) return 0
    return indentation
  }

  fun getMinimalIndentation(content: HCLHeredocContent): Int? {
    val children = content.node.getChildren(null)
    return children.map { it.chars.indentation }.min()
  }

  fun getValue(content: HCLHeredocContent, trimFirst: Int = 0): String {
    val builder = StringBuilder()
    content.linesRaw.forEach { builder.append(it.substring(trimFirst)) }
    // Last line EOL is not part of value
    builder.removeSuffix("\n")
    return builder.toString()
  }

  fun getLines(content: HCLHeredocContent): List<String> {
    val parent = content.parent
    val indentation: Int =
        if (parent is HCLHeredocLiteral) parent.indentation ?: 0
        else 0
    val children = content.node.getChildren(null)
    return children.mapTo(SmartList<String>()) { it.text.substring(indentation) }
  }

  fun getLinesRaw(content: HCLHeredocContent): List<CharSequence> {
    val children = content.node.getChildren(null)
    return children.mapTo(SmartList<CharSequence>()) { it.chars }
  }

  fun getLinesCount(content: HCLHeredocContent): Int {
    val node = content.node
    var cn: ASTNode? = node.firstChildNode
    var counter: Int = 0
    while (cn != null) {
      cn = cn.treeNext
      counter++
    }
    return counter
  }

  fun getValue(literal: HCLBooleanLiteral): Boolean {
    return literal.textMatches("true")
  }

  fun getValue(literal: HCLNumberLiteral): Double {
    val text = literal.text
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
    when(suffix.length) {
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


  fun getId(identifier: HCLIdentifier): String {
    return identifier.text
  }
}
