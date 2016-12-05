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
package org.intellij.plugins.hil.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLElement
import org.intellij.plugins.hcl.psi.getNameElementUnquoted
import org.intellij.plugins.hcl.terraform.config.model.Module
import org.intellij.plugins.hcl.terraform.config.model.Variable
import org.intellij.plugins.hcl.terraform.config.model.getTerraformModule

fun getTerraformModule(element: ILExpression): Module? {
  val host = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: return null
  if (host !is HCLElement) return null
  val module = host.getTerraformModule()
  return module
}

fun getLocalDefinedVariables(element: ILExpression): List<Variable> {
  return getTerraformModule(element)?.getAllVariables()?.map { it.first } ?: emptyList()
}

fun getProvisionerResource(position: ILExpression): HCLBlock? {
  val host = InjectedLanguageManager.getInstance(position.project).getInjectionHost(position) ?: return null

  // For now 'self' allowed only for provisioners inside resources
  return if (host is HCLElement) getProvisionerResource(host) else null
}

fun getProvisionerResource(host: HCLElement): HCLBlock? {
  val provisioner = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java) ?: return null
  if (provisioner.getNameElementUnquoted(0) == "connection") return getProvisionerResource(provisioner)
  if (provisioner.getNameElementUnquoted(0) != "provisioner") return null
  val resource = PsiTreeUtil.getParentOfType(provisioner, HCLBlock::class.java, true) ?: return null
  if (resource.getNameElementUnquoted(0) != "resource") return null
  return resource
}

fun getConnectionResource(host: HCLElement): HCLBlock? {
  val provisioner = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java) ?: return null
  if (provisioner.getNameElementUnquoted(0) != "connection") return null
  val resource = PsiTreeUtil.getParentOfType(provisioner, HCLBlock::class.java, true) ?: return null
  if (resource.getNameElementUnquoted(0) != "resource") return null
  return resource
}

fun getResource(position: ILExpression): HCLBlock? {
  val host = InjectedLanguageManager.getInstance(position.project).getInjectionHost(position) ?: return null

  // For now 'self' allowed only for provisioners inside resources

  val resource = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java, true) ?: return null
  if (resource.getNameElementUnquoted(0) != "resource") return null
  return resource
}

fun getDataSource(position: ILExpression): HCLBlock? {
  val host = InjectedLanguageManager.getInstance(position.project).getInjectionHost(position) ?: return null

  val dataSource = PsiTreeUtil.getParentOfType(host, HCLBlock::class.java, true) ?: return null
  if (dataSource.getNameElementUnquoted(0) != "data") return null
  return dataSource
}


fun <T : PsiElement> PsiElement.getNthChild(n: Int, clazz: Class<ILExpression>): T? {
  var child: PsiElement? = this.firstChild
  var i: Int = 0
  while (child != null) {
    if (clazz.isInstance(child)) {
      i++
      @Suppress("UNCHECKED_CAST")
      if (i == n) return child as T?
    }
    child = child.nextSibling
  }
  return null
}