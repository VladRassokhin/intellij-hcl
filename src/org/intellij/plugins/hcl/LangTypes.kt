package org.intellij.plugins.hcl

import com.intellij.psi.tree.IElementType

open class HCLElementType(debugName: String) : IElementType(debugName, HCLLanguage)
open class HCLTokenType(debugName: String) : IElementType(debugName, HCLLanguage)

