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

    val node = element.node

    val children = node.getChildren(null)
    val prefixStartString = children[startString].text.substring(0, range.startOffset - ranges[startString].startOffset)
    val suffixEndString = children[endString].text.substring(range.endOffset - ranges[endString].startOffset)

    var afterNode: ASTNode? = children[endString].treeNext

    //////////
    // Prepare new lines content

    val newText = prefixStartString + newContent + suffixEndString
    val newLines: MutableList<String> = getReplacementLines(newText)

    //////////
    // Replace nodes
    // TODO: Try LeafElement.replaceWithText or LeafElement.rawReplaceWithText to not lose HIL injection fragment editing window

    node.removeRange(children[startString], afterNode)
    for (line in newLines) {
      node.addLeaf(HCLElementTypes.HD_LINE, line, afterNode)
    }
    return element
  }

  private fun getReplacementLines(newText: String): MutableList<String> {
    val newLines: MutableList<String> = when {
      newText == "" -> SmartList()
      else -> SmartList<String>(StringUtil.split(newText.ensureHaveSuffix("\n"), "\n", false, false).dropLast(1))
    }
    return newLines
  }

  override fun handleContentChange(element: HCLHeredocContent, newContent: String): HCLHeredocContent {
    // Do simple full replacement
    val newLines = getReplacementLines(newContent)

    val node = element.node
    node.removeRange(node.firstChildNode, null)
    for (line in newLines) {
      node.addLeaf(HCLElementTypes.HD_LINE, line, null)
    }
    return element
  }
}
