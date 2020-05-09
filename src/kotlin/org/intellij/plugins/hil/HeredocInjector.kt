/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
package org.intellij.plugins.hil;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.PsiElement;
import org.intellij.plugins.hcl.psi.HCLHeredocContent;
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.ElementManipulators
import com.google.common.collect.TreeRangeSet
import com.google.common.collect.Range
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.impl.AbstractFileType


class HeredocInjector : MultiHostInjector{

  override fun getLanguagesToInject(registrar: MultiHostRegistrar, injectionHost: PsiElement) {
    if (injectionHost !is HCLHeredocContent)
      return
    if (injectionHost.textLength == 0)
      return

    val rangeSet = TreeRangeSet.create<Int>()
    val range = ElementManipulators.getValueTextRange(injectionHost)
    val interpolationRanges = ILLanguageInjector.Companion.getILRangesInText(injectionHost.text)
    rangeSet.add(Range.closed(range.startOffset, range.endOffset))
    interpolationRanges.forEach { interpolation ->
      rangeSet.remove(Range.closed(interpolation.startOffset, interpolation.endOffset))
    }
    val lang = getHeredocLanguage(injectionHost.text)

    if (lang is Language && !rangeSet.isEmpty) {
      registrar.startInjecting(lang);
      rangeSet.asRanges().forEach{ leftovers ->
        val lower = leftovers.lowerEndpoint()
        val upper = leftovers.upperEndpoint()
        registrar.addPlace("", "", (injectionHost as PsiLanguageInjectionHost), TextRange(lower, upper) )
      }
      registrar.doneInjecting()
    }

    if(!interpolationRanges.isEmpty()){
      interpolationRanges.forEach { interpolation ->
        registrar.startInjecting(HILLanguage)
        registrar.addPlace("", "", (injectionHost as PsiLanguageInjectionHost), TextRange(interpolation.startOffset, interpolation.endOffset) )
        registrar.doneInjecting()
      }

    }
    return
  }

  fun getHeredocLanguage(text: String) : Language?{
    // If it starts with a curly it should be JSON
    if (text.startsWith("{")){
      val fileType = FileTypeManager.getInstance().getFileTypeByExtension("json")
      if (fileType !is AbstractFileType) {
        return (fileType as LanguageFileType).language
      }
    }
    // If it starts with a shebang it should be a shell script
    if (text.startsWith("#!")){
      val fileType = FileTypeManager.getInstance().getFileTypeByExtension("sh")
      if (fileType !is AbstractFileType) {
        return (fileType as LanguageFileType).language
      }
    }

    return null
  }

  override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
    return mutableListOf(HCLHeredocContent::class.java)
  }


}
