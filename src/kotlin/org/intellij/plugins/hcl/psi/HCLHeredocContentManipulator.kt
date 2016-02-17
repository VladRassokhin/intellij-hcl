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
package org.intellij.plugins.hcl.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SmartList
import org.intellij.plugins.hcl.HCLElementTypes
import org.intellij.plugins.hcl.terraform.config.model.ensureHaveSuffix

class HCLHeredocContentManipulator : AbstractElementManipulator<HCLHeredocContent>() {

  // There two major cases:
  // 1. all content replaced with actual diff very small (full heredoc injection):
  // 1.1 One line modified
  // 1.2 One line added
  // 1.3 One line removed
  // 2. one line content (probably not all) replaced with any diff (HIL injection)
  // This cases should work quite fast

  @Throws(IncorrectOperationException::class)
  override fun handleContentChange(element: HCLHeredocContent, range: TextRange, newContent: String): HCLHeredocContent {
    if (range.length == 0 && element.linesCount == 0) {
      // Replace empty with something
      return handleContentChange(element, newContent)
    }

    //////////
    // Calculate affected strings (based on offsets)

    var offset: Int = 0;
    val lines = element.lines
    val ranges = lines.map {
      val r: TextRange = TextRange.from(offset, it.length);
      offset += it.length;
      r
    }

    val startString: Int
    val endString: Int

    val linesToChange = ranges.indices.filter { ranges[it].intersects(range) }
    assert(linesToChange.isNotEmpty())
    startString = linesToChange.first()
    endString = linesToChange.last()

    val node = element.node as TreeElement

    val children = node.getChildren(null)
    val prefixStartString = children[startString].text.substring(0, range.startOffset - ranges[startString].startOffset)
    val suffixEndString = children[endString].text.substring(range.endOffset - ranges[endString].startOffset).removeSuffix("\n")

    //////////
    // Prepare new lines content

    val newText = prefixStartString + newContent + suffixEndString
    val newLines: MutableList<String> = getReplacementLines(newText)

    //////////
    // Replace nodes

    var stopNode: ASTNode? = children[endString].treeNext
    var iter: ASTNode? = children[startString]
    for (line in newLines) {
      if (iter != null && iter != stopNode) {
        // Replace existing lines
        val next = iter.treeNext
        if (iter.text != line) {
          // Replace node text
          (iter as LeafElement).replaceWithText(line)
        }
        iter = next
      } else {
        // Add new lines to end
        node.addLeaf(HCLElementTypes.HD_LINE, line, stopNode)
      }
    }
    // Remove extra lines
    if (iter != null && iter != stopNode) {
      node.removeRange(iter, stopNode)
    }
    return element
  }

  companion object {
    fun getReplacementLines(newText: String): MutableList<String> {
      if (newText == "") (return SmartList())
      val newLines: MutableList<String>
      newLines = SmartList<String>(StringUtil.split(newText, "\n", false, false))
      newLines[newLines.lastIndex] = newLines[newLines.lastIndex].ensureHaveSuffix("\n")
      return newLines
    }
  }

  override fun handleContentChange(element: HCLHeredocContent, newContent: String): HCLHeredocContent {
    // Do simple full replacement
    val newLines = getReplacementLines(newContent)

    val node = element.node
    if (node.firstChildNode != null) {
      node.removeRange(node.firstChildNode, null)
    }
    for (line in newLines) {
      node.addLeaf(HCLElementTypes.HD_LINE, line, null)
    }
    return element
  }

  override fun getRangeInElement(element: HCLHeredocContent): TextRange {
    if (element.textLength == 0) return TextRange.EMPTY_RANGE
    return TextRange.from(0, element.textLength - 1)
  }
}
