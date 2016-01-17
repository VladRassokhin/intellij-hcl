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
package org.intellij.plugins.hcl.terraform.config.codeinsight

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import org.intellij.plugins.hcl.Icons
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType
import org.intellij.plugins.hcl.terraform.config.model.Type
import javax.swing.Icon

class TerraformLookupElementRenderer : LookupElementRenderer<LookupElement>() {
  override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
    presentation.itemText = element.lookupString
    val obj = element.`object`
    if (obj is PropertyOrBlockType) {
      if (obj.property != null) {
        presentation.icon = Icons.Property
        //        presentation.isStrikeout = obj.property.deprecated;
        presentation.isItemTextBold = obj.property.required;
        presentation.isStrikeout = obj.property.deprecated != null;
        presentation.tailText = obj.property.description;
        presentation.setTypeText(obj.property.type.name, getTypeIcon(obj.property.type));
      } else if (obj.block != null) {
        //        presentation.icon = Icons.Property
        //        presentation.isStrikeout = obj.property.deprecated;
        presentation.isItemTextBold = obj.block.required;
        presentation.isStrikeout = obj.block.deprecated != null;
//        presentation.typeText = "Block";
        presentation.tailText = obj.block.description;
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun getTypeIcon(type: Type): Icon? {
    // TODO: Implement
    return null
  }
}