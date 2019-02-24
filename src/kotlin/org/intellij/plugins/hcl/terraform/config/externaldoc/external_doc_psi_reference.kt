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
package org.intellij.plugins.hcl.terraform.config.externaldoc

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ProcessingContext
import org.intellij.plugins.hcl.psi.HCLBlock
import org.intellij.plugins.hcl.psi.HCLIdentifier
import org.intellij.plugins.hcl.psi.HCLStringLiteral
import org.intellij.plugins.hcl.terraform.config.model.TypeModel
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider
import org.intellij.plugins.hil.psi.ILVariable

class ExternalUrlReference(private val element: PsiElement, private val rangeInElement: TextRange,  val url: String) : PsiReference {

  override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException()

  override fun getCanonicalText(): String = url

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = rangeInElement

  override fun getVariants(): Array<PsiReference> = PsiReference.EMPTY_ARRAY

  override fun handleElementRename(newElementName: String?): PsiElement = throw IncorrectOperationException()

  override fun isReferenceTo(element: PsiElement?): Boolean = false

  override fun isSoft(): Boolean = true

  override fun resolve(): PsiElement? = MyFakePsiElement()

  internal inner class MyFakePsiElement : FakePsiElement() {
    override fun getParent(): PsiElement = element

    fun getUrl(): String = url

    override fun navigate(requestFocus: Boolean) = BrowserUtil.browse(url)
  }
}

abstract class ExternalDocReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    if (element is HCLStringLiteral) { // TODO do we need this check?
      val text = element.text
      if (text.length > 2) {
        val textRange = TextRange(1, text.length - 1)
        val url = getUrl(TypeModelProvider.getModel(element.project), textRange.substring(text))
        if (url != null) return arrayOf(ExternalUrlReference(element, textRange, url))
      }
    }
    return PsiReference.EMPTY_ARRAY
  }
  
  abstract fun getUrl(model: TypeModel, type: String): String?
}

object BackendTypeReferenceProvider : ExternalDocReferenceProvider() {
  override fun getUrl(model: TypeModel, type: String): String? = if (model.getBackendType(type) == null) null else urlForBackendTypeDoc(type)
}

object DataSourceTypeReferenceProvider : ExternalDocReferenceProvider() {
  override fun getUrl(model: TypeModel, type: String): String? {
    val dataSource = model.getDataSourceType(type)
    return if (dataSource == null) null else urlForDataSourceTypeDoc(dataSource.provider.type, type)
  }
}

object FunctionReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    if (element is ILVariable) {  // TODO do we need this check
      val name = element.text
      val function = TypeModelProvider.getModel(element.project).getFunction(name)
      if (function != null) return arrayOf(ExternalUrlReference(element, TextRange.allOf(name), urlForFunctionDoc(function.name)))
    }
    return PsiReference.EMPTY_ARRAY
  }
}

object ProviderTypeReferenceProvider : ExternalDocReferenceProvider() {
  override fun getUrl(model: TypeModel, type: String): String? = if (model.getProviderType(type) == null) null else urlForProviderTypeDoc(type)
}

object ProvisionerTypeReferenceProvider : ExternalDocReferenceProvider() {
  override fun getUrl(model: TypeModel, type: String): String? = if (model.getProvisionerType(type) == null) null else urlForProvisionerTypeDoc(type)
}

object ResourceTypeReferenceProvider : ExternalDocReferenceProvider() {
  override fun getUrl(model: TypeModel, type: String): String? {
    val resource = model.getResourceType(type)
    return if (resource == null) null else urlForResourceTypeDoc(resource.provider.type, type)
  }
}

object KeywordReferenceProvider : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    if (element is HCLIdentifier) {
      val text = element.text
      if (checkKeyword(text, element)) {
        val url = urlForKeywordDoc(text)
        if (url != null) return arrayOf(ExternalUrlReference(element, TextRange.allOf(text), url))
      }
    }
    return PsiReference.EMPTY_ARRAY
  }

  private fun checkKeyword(text: String, element: PsiElement): Boolean {
    return when (text) {
      "backend" -> checkOuterBlock(element,"terraform" )
      "provisioner" -> checkOuterBlock(element,"resource" )
      else -> true
    }
  }

  private fun checkOuterBlock(element: PsiElement, expectedBlockType: String): Boolean {
    val outerBlock = element.parent?.parent?.parent
    return outerBlock is HCLBlock && outerBlock.nameElements[0].text == expectedBlockType
  }
}
