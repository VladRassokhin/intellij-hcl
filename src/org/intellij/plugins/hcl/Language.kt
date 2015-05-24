package org.intellij.plugins.hcl

import com.intellij.lang.Language

object HCLLanguage : Language("HCL") {
  override fun isCaseSensitive() = true
}

