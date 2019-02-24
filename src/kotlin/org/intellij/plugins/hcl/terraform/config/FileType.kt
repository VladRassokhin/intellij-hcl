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
package org.intellij.plugins.hcl.terraform.config

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import org.intellij.plugins.hcl.Icons


object TerraformFileType : LanguageFileType(TerraformLanguage) {
  val DEFAULT_EXTENSION: String = "tf"
  val TFVARS_EXTENSION: String = "tfvars"

  override fun getIcon() = Icons.FileTypes.Terraform

  override fun getDefaultExtension() = DEFAULT_EXTENSION

  override fun getDescription() = "Terraform config files"

  override fun getName() = "Terraform"

  override fun toString() = name
}

class TerraformFileTypeFactory : FileTypeFactory() {
  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(TerraformFileType, TerraformFileType.DEFAULT_EXTENSION)
    consumer.consume(TerraformFileType, TerraformFileType.TFVARS_EXTENSION)
  }
}
