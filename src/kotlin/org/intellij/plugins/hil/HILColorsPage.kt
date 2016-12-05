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
package org.intellij.plugins.hil

import com.intellij.application.options.colors.InspectionColorSettingsPage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.psi.codeStyle.DisplayPriority
import com.intellij.psi.codeStyle.DisplayPrioritySortable
import org.intellij.plugins.hcl.Icons
import javax.swing.Icon

class HILColorPage : ColorSettingsPage, InspectionColorSettingsPage, DisplayPrioritySortable {

  companion object {
    private val descriptors: Array<out AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Braces and Operators//Parentheses", HILSyntaxHighlighterFactory.TIL_PARENS),
        AttributesDescriptor("Braces and Operators//Braces", HILSyntaxHighlighterFactory.TIL_BRACES),
        AttributesDescriptor("Braces and Operators//Brackets", HILSyntaxHighlighterFactory.TIL_BRACKETS),
        AttributesDescriptor("Braces and Operators//Comma", HILSyntaxHighlighterFactory.TIL_COMMA),
        AttributesDescriptor("Braces and Operators//Operation sign", HILSyntaxHighlighterFactory.TIL_OPERATOR),
        AttributesDescriptor("Braces and Operators//Dot", HILSyntaxHighlighterFactory.TIL_DOT),
        AttributesDescriptor("Number", HILSyntaxHighlighterFactory.TIL_NUMBER),
        AttributesDescriptor("String", HILSyntaxHighlighterFactory.TIL_STRING),
        AttributesDescriptor("Keyword", HILSyntaxHighlighterFactory.TIL_KEYWORD),
        AttributesDescriptor("Identifier", HILSyntaxHighlighterFactory.TIL_IDENTIFIER),
        AttributesDescriptor("Predefined scope", HILSyntaxHighlighterFactory.TIL_PREDEFINED_SCOPE),
        AttributesDescriptor("Resource type reference", HILSyntaxHighlighterFactory.TIL_RESOURCE_TYPE_REFERENCE),
        AttributesDescriptor("Resource instance reference", HILSyntaxHighlighterFactory.TIL_RESOURCE_INSTANCE_REFERENCE),
        AttributesDescriptor("Property reference", HILSyntaxHighlighterFactory.TIL_PROPERTY_REFERENCE),
        AttributesDescriptor("Valid escape sequence", HILSyntaxHighlighterFactory.TIL_VALID_ESCAPE),
        AttributesDescriptor("Invalid escape sequence", HILSyntaxHighlighterFactory.TIL_INVALID_ESCAPE)
    )
    private val additional: Map<String, TextAttributesKey> = mapOf(
        "rt" to HILSyntaxHighlighterFactory.TIL_RESOURCE_TYPE_REFERENCE,
        "ri" to HILSyntaxHighlighterFactory.TIL_RESOURCE_INSTANCE_REFERENCE,
        "pr" to HILSyntaxHighlighterFactory.TIL_PROPERTY_REFERENCE,
        "s" to HILSyntaxHighlighterFactory.TIL_PREDEFINED_SCOPE
    )
  }

  override fun getIcon(): Icon? {
    return Icons.FileTypes.HIL
  }

  override fun getHighlighter(): SyntaxHighlighter {
    return SyntaxHighlighterFactory.getSyntaxHighlighter(HILLanguage, null, null)
  }

  override fun getDemoText(): String {
    return "\${\"interpolati\\o\\n\".example.call(10, \"a\", \n" +
        "<s>var</s>.foo, <s>path</s>.module, 1 - 0 + (11 * 4) / 2 % 1, \n" +
        "\ttrue || !false, false && !true, true ? 1 : 2, null,\n" +
        "<rt>aws_instance</rt>.<ri>inst</ri>.<pr>availability_zone</pr>[0])}"
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
    return HILLanguage.displayName
  }

  override fun getPriority(): DisplayPriority? {
    return DisplayPriority.LANGUAGE_SETTINGS
  }
}
