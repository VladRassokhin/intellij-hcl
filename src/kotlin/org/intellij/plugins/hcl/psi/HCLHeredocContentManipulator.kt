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
import org.intellij.plugins.hcl.HCLElementTypes

class HCLHeredocContentManipulator : AbstractElementManipulator<HCLHeredocContent>() {
  @Throws(IncorrectOperationException::class)
  override fun handleContentChange(element: HCLHeredocContent, range: TextRange, newContent: String): HCLHeredocContent {
    if (range.length == 0) return element

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

    node.removeRange(children[startString], afterNode)

    val newText = prefixStartString + newContent + suffixEndString
    val newLines = StringUtil.split(newText, "\n", false, false)
    // Remove last empty line
    newLines.forEachIndexed { i, line ->
      if (i != newLines.lastIndex || line.isNotEmpty()) {
        node.addLeaf(HCLElementTypes.HD_LINE, line, afterNode)
      }
    }
    return element
  }

  override fun handleContentChange(element: HCLHeredocContent, newContent: String): HCLHeredocContent {
    // Do simple full replacement
    val node = element.node
    node.removeRange(node.firstChildNode, null)
    val newLines = StringUtil.split(newContent, "\n", false, false)
    for (line in newLines) {
      node.addLeaf(HCLElementTypes.HD_LINE, line, null)
    }
    return element
  }
}
