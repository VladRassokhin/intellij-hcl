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

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.intellij.plugins.hcl.terraform.config.externalDoc.FunctionReferenceProvider
import org.intellij.plugins.hil.codeinsight.HILCompletionContributor

class ILReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(psiElement(ILVariable::class.java)
        .withParent(HILCompletionContributor.ILSE_FROM_KNOWN_SCOPE), ILSelectFromScopeReferenceProvider)

    registrar.registerReferenceProvider(psiElement(ILVariable::class.java)
        .withParent(HILCompletionContributor.ILSE_NOT_FROM_KNOWN_SCOPE), ILSelectFromSomethingReferenceProvider)

    registrar.registerReferenceProvider(psiElement(ILLiteralExpression::class.java)
        .withParent(HILCompletionContributor.ILISE_NOT_FROM_KNOWN_SCOPE), ILSelectFromSomethingReferenceProvider)

    registrar.registerReferenceProvider(psiElement(ILVariable::class.java)
        .withParent(HILCompletionContributor.ILSE_FROM_KNOWN_SCOPE), ILScopeReferenceProvider)

    registrar.registerReferenceProvider(psiElement(ILVariable::class.java)
        .withParent(HILCompletionContributor.ILSE_DATA_SOURCE), ILSelectFromSomethingReferenceProvider)

    registrar.registerReferenceProvider(psiElement(ILVariable::class.java)
        .withParent(ILMethodCallExpression::class.java), FunctionReferenceProvider)
  }
}
