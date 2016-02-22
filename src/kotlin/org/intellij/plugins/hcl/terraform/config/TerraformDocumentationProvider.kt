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
package org.intellij.plugins.hcl.terraform.config

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import getNameElementUnquoted
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLIdentifier
import org.intellij.plugins.hcl.psi.HCLProperty
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.codeinsight.ModelHelper
import org.intellij.plugins.hcl.terraform.config.model.TypeModel

class TerraformDocumentationProvider : AbstractDocumentationProvider() {
  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    if (element is HCLProperty) {
      return "Property ${element.name}"
    }
    return null
  }

  override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
    if (element is HCLProperty) {
      if (element.containingFile.fileType != TerraformFileType) return null

      val pp = element.parent?.parent
      if (pp !is HCLBlock) return null
      val properties = ModelHelper.getBlockProperties(pp)
      val property = properties.firstOrNull { it.property?.name == element.name }?.property ?: return "Unknown property ${element.name}"
      return buildString {
        append("Property ")
        append(element.name)
        append(" (")
        append(property.type.name)
        append(")")
        if (property.description != null) {
          append("<br/>")
          append(property.description)
        }
      }
    } else if (element is HCLBlock) {
      if (element.containingFile.fileType != TerraformFileType) return null

      val pp = element.parent?.parent
      if (pp !is HCLBlock) {
        val block = TypeModel.RootBlocks.firstOrNull { it.literal == element.getNameElementUnquoted(0) } ?: return null
        return buildString {
          append("Block ")
          append(element.name)
          if (block.description != null) {
            append("<br/>")
            append(block.description)
          }
        }
      }
      val properties = ModelHelper.getBlockProperties(pp)
      val block = properties.firstOrNull { it.block?.literal == element.getNameElementUnquoted(0) }?.block ?: return "Unknown block ${element.name}"
      return buildString {
        append("Block ")
        append(element.name)
        if (block.description != null) {
          append("<br/>")
          append(block.description)
        }
      }
    } else if (element is HCLStringLiteral || element is HCLIdentifier) {
      if (element.containingFile.fileType != TerraformFileType) return null
      val parent = element.parent
      if (parent is PsiNameIdentifierOwner && parent.nameIdentifier === element) {
        return generateDoc(parent, originalElement);
      }
    }
    return null
  }
}
