package org.intellij.plugins.hcl.psi.impl

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.plugins.hcl.HCLLanguage
import org.intellij.plugins.hcl.psi.HCLFile
import org.intellij.plugins.hcl.psi.HCLValue

public class HCLFileImpl(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, HCLLanguage), HCLFile {

  override fun getFileType(): FileType {
    return getViewProvider().getVirtualFile().getFileType()
  }

  override fun toString(): String {
    val virtualFile = getVirtualFile()
    return "HCLFile: " + (if (virtualFile != null) virtualFile.getName() else "<unknown>")
  }
}
