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
package org.intellij.plugins.hcl

import com.google.common.collect.ImmutableMap
import com.intellij.application.options.colors.InspectionColorSettingsPage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.psi.codeStyle.DisplayPriority
import com.intellij.psi.codeStyle.DisplayPrioritySortable
import javax.swing.Icon

class HCLColorsPage : ColorSettingsPage, InspectionColorSettingsPage, DisplayPrioritySortable {

  companion object {
    private val descriptors: Array<out AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Brackets", HCLSyntaxHighlighterFactory.HCL_BRACKETS),
        AttributesDescriptor("Braces", HCLSyntaxHighlighterFactory.HCL_BRACES),
        AttributesDescriptor("Comma", HCLSyntaxHighlighterFactory.HCL_COMMA),
        //        AttributesDescriptor("Colon", HCLSyntaxHighlighterFactory.HCL_COLON),
        AttributesDescriptor("Number", HCLSyntaxHighlighterFactory.HCL_NUMBER),
        AttributesDescriptor("String", HCLSyntaxHighlighterFactory.HCL_STRING),
        AttributesDescriptor("Keyword", HCLSyntaxHighlighterFactory.HCL_KEYWORD),
        AttributesDescriptor("Line comment", HCLSyntaxHighlighterFactory.HCL_LINE_COMMENT),
        AttributesDescriptor("Block comment", HCLSyntaxHighlighterFactory.HCL_BLOCK_COMMENT),
        AttributesDescriptor("Identifier", HCLSyntaxHighlighterFactory.HCL_IDENTIFIER),
        AttributesDescriptor("Property key", HCLSyntaxHighlighterFactory.HCL_PROPERTY_KEY),
        AttributesDescriptor("Valid escape sequence", HCLSyntaxHighlighterFactory.HCL_VALID_ESCAPE),
        AttributesDescriptor("Invalid escape sequence", HCLSyntaxHighlighterFactory.HCL_INVALID_ESCAPE)
    )
    private val additional: Map<String, TextAttributesKey> = ImmutableMap.of("propertyKey", HCLSyntaxHighlighterFactory.HCL_PROPERTY_KEY)
  }

  override fun getIcon(): Icon? {
    return Icons.FileTypes.HCL
  }

  override fun getHighlighter(): SyntaxHighlighter {
    return SyntaxHighlighterFactory.getSyntaxHighlighter(HCLLanguage, null, null)
  }

  override fun getDemoText(): String {
    return """
/*
  Here's simple HCL code to show you syntax highlighter
*/
<propertyKey>name</propertyKey> = value
// Simple single line comment
block 'name' {
  <propertyKey>array</propertyKey> = [ 'a', 100, "b", 10.5e-42, true, false ]
  <propertyKey>empty_array</propertyKey> = []
  <propertyKey>empty_object</propertyKey> = {}
  # Yet another comment style
  <propertyKey>strings</propertyKey> = {
    <propertyKey>"something"</propertyKey> = "\"Quoted Yep!\""
    <propertyKey>bad</propertyKey> = "Invalid escaping:\c"
    <propertyKey>'good'</propertyKey> = "Valid escaping:\"\n\"\"
  }
}"""
  }

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
    return additional;
  }

  override fun getAttributeDescriptors(): Array<out AttributesDescriptor> {
    return descriptors;
  }

  override fun getColorDescriptors(): Array<out ColorDescriptor> {
    return ColorDescriptor.EMPTY_ARRAY
  }

  override fun getDisplayName(): String {
    return HCLLanguage.displayName
  }

  override fun getPriority(): DisplayPriority? {
    return DisplayPriority.LANGUAGE_SETTINGS
  }
}