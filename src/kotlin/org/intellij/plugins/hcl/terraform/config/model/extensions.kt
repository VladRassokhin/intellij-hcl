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
package org.intellij.plugins.hcl.terraform.config.model

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.testFramework.LightVirtualFile
import org.intellij.plugins.hcl.psi.*
import org.intellij.plugins.hil.psi.ILExpression

fun HCLElement.getTerraformModule(): Module {
  val file = this.containingFile.originalFile
  assert(file is HCLFile)
  return Module.getModule(file)
}

fun PsiElement.getTerraformSearchScope(): GlobalSearchScope {
  val file = this.containingFile.originalFile
  var directory = file.containingDirectory
  if (directory == null) {
    if (this is ILExpression) {
      directory = InjectedLanguageManager.getInstance(project).getTopLevelFile(this)?.containingDirectory
    }
  }
  if (directory == null) {
    // File only in-memory, assume as only file in module
    var vf: VirtualFile? = file.virtualFile
    if (vf is LightVirtualFile) {
      vf = vf.originalFile?:vf
    }
    val parent = vf?.parent ?: return GlobalSearchScope.fileScope(file)
    return GlobalSearchScopes.directoryScope(file.project, parent, false)
  } else {
    return GlobalSearchScopes.directoryScope(directory, false)
  }
}

fun HCLProperty.toProperty(type: PropertyType): Property {
  return Property(type, this.value)
}

fun HCLBlock.getProviderFQName(): String? {
  val tp = this.getNameElementUnquoted(1) ?: return null
  val value = this.`object`?.findProperty("alias")?.value
  val als = when (value) {
    is HCLStringLiteral -> value.value
    is HCLIdentifier -> value.id
    else -> null
  }
  if (als != null) {
    return "$tp.$als"
  } else {
    return tp
  }
}

fun HCLValue?.getValueType(): Type? {
  if (this == null) return null
  return when (this) {
    is HCLObject -> Types.Object
    is HCLArray -> Types.Array
    is HCLIdentifier -> Types.Identifier
    is HCLStringLiteral -> Types.String
    is HCLHeredocLiteral -> Types.String
    is HCLNumberLiteral -> Types.Number
    is HCLBooleanLiteral -> Types.Boolean
    is HCLNullLiteral -> Types.Null
    else -> null
  }
}

fun String.ensureHavePrefix(prefix: String) = if (this.startsWith(prefix)) this else (prefix + this)
fun String.ensureHaveSuffix(suffix: String) = if (this.endsWith(suffix)) this else (this + suffix)
