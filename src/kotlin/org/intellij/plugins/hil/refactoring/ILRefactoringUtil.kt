/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.intellij.plugins.hil.refactoring

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.PsiEquivalenceUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import org.intellij.plugins.hil.HILLanguage
import org.intellij.plugins.hil.psi.ILExpression
import org.intellij.plugins.hil.psi.ILRecursiveVisitor
import java.util.*
import java.util.regex.Pattern

object ILRefactoringUtil {
  fun <T : PsiElement> findElementInRange(file: PsiFile, startOffset: Int, endOffset: Int, klass: Class<T>): T {
    return CodeInsightUtilCore.findElementInRange(file, startOffset, endOffset, klass, HILLanguage)
  }

  fun findExpressionInRange(file: PsiFile, startOffset: Int, endOffset: Int): ILExpression? {
    if (!file.viewProvider.languages.contains(HILLanguage)) return null
    return findElementInRange(file, startOffset, endOffset, ILExpression::class.java)
  }

  fun getSelectedExpression(project: Project,
                            file: PsiFile,
                            element1: PsiElement,
                            element2: PsiElement): ILExpression? {
    var parent = PsiTreeUtil.findCommonParent(element1, element2)
    if (parent != null && parent !is ILExpression) {
      parent = PsiTreeUtil.getParentOfType(parent, ILExpression::class.java)
    }
    if (parent == null) {
      return null
    }
    if (parent !is ILExpression) {
      return null
    }
    if (element1 === PsiTreeUtil.getDeepestFirst(parent) && element2 === PsiTreeUtil.getDeepestLast(parent)) {
      return parent
    }
    return null
  }

  fun getOccurrences(pattern: PsiElement, context: PsiElement?): List<PsiElement> {
    if (context == null) {
      return emptyList()
    }
    val occurrences = ArrayList<PsiElement>()
    context.acceptChildren(object : ILRecursiveVisitor() {
      override fun visitElement(element: PsiElement?) {
        if (element == null) return
        if (PsiEquivalenceUtil.areElementsEquivalent(element, pattern)) {
          occurrences.add(element)
          return
        }
        super.visitElement(element)
      }
    })
    return occurrences
  }


  private fun deleteNonLetterFromString(string: String): String {
    val pattern = Pattern.compile("[^a-zA-Z_]+")
    val matcher = pattern.matcher(string)
    return matcher.replaceAll("_")
  }

  fun generateNames(name: String): Collection<String> {
    @Suppress("NAME_SHADOWING")
    var name = name
    name = StringUtil.decapitalize(deleteNonLetterFromString(StringUtil.unquoteString(name.replace('.', '_'))))
    if (name.startsWith("get")) {
      name = name.substring(3)
    } else if (name.startsWith("is")) {
      name = name.substring(2)
    }
    while (name.startsWith("_")) {
      name = name.substring(1)
    }
    while (name.endsWith("_")) {
      name = name.substring(0, name.length - 1)
    }
    val length = name.length
    val possibleNames = LinkedHashSet<String>()
    for (i in 0..length - 1) {
      if (Character.isLetter(name[i]) && (i == 0 || name[i - 1] == '_' || Character.isLowerCase(name[i - 1]) && Character.isUpperCase(name[i]))) {
        val candidate = StringUtil.decapitalize(name.substring(i))
        if (candidate.length < 25) {
          possibleNames.add(candidate)
        }
      }
    }
    // prefer shorter names
    val reversed = ArrayList(possibleNames)
    Collections.reverse(reversed)
    return ContainerUtil.map(reversed) { name1 ->
      @Suppress("NAME_SHADOWING")
      var name1 = name1
      if (name1.indexOf('_') == -1) {
        return@map name1
      }
      name1 = StringUtil.capitalizeWords(name1, "_", true, true)
      StringUtil.decapitalize(name1.replace("_".toRegex(), ""))
    }
  }

  fun generateNamesByType(name: String): Collection<String> {
    var name = name
    val possibleNames = LinkedHashSet<String>()
    name = StringUtil.decapitalize(deleteNonLetterFromString(name.replace('.', '_')))
    name = toUnderscoreCase(name)
    possibleNames.add(name.substring(0, 1))
    possibleNames.add(name)
    return possibleNames
  }

  fun toUnderscoreCase(name: String): String {
    val buffer = StringBuilder()
    val length = name.length

    for (i in 0..length - 1) {
      val ch = name[i]
      if (ch != '-') {
        buffer.append(Character.toLowerCase(ch))
      } else {
        buffer.append("_")
      }

      if (Character.isLetterOrDigit(ch)) {
        if (Character.isUpperCase(ch)) {
          if (i + 2 < length) {
            val chNext = name[i + 1]
            val chNextNext = name[i + 2]

            if (Character.isUpperCase(chNext) && Character.isLowerCase(chNextNext)) {

              buffer.append('_')
            }
          }
        } else if (Character.isLowerCase(ch) || Character.isDigit(ch)) {
          if (i + 1 < length) {
            val chNext = name[i + 1]
            if (Character.isUpperCase(chNext)) {
              buffer.append('_')
            }
          }
        }
      }
    }
    return buffer.toString()
  }

}