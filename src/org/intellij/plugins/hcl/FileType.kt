package org.intellij.plugins.hcl

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object HCLFileType: LanguageFileType(HCLLanguage) {
  public val DEFAULT_EXTENSION: String = "hcl"

  override fun getIcon() = null

  override fun getDefaultExtension() = DEFAULT_EXTENSION

  override fun getDescription() = "HCL files"

  override fun getName() = "HCL"

}

class HCLFileTypeFactory : FileTypeFactory(){
  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(HCLFileType, HCLFileType.DEFAULT_EXTENSION)
  }
}