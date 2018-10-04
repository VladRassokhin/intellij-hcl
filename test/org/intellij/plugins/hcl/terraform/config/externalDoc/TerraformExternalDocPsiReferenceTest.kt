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
package org.intellij.plugins.hcl.terraform.config.externalDoc

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.intellij.plugins.hcl.HCLBundle.message
import org.intellij.plugins.hcl.terraform.config.TerraformFileType
import org.junit.Test

class TerraformExternalDocPsiReferenceTest : LightCodeInsightFixtureTestCase() {
  private fun doTest(text: String, expectedUrl: String?, expectedQuickNavigateInfo: String = message("open.documentation.in.browser")) {
    myFixture.configureByText(TerraformFileType, text)
    val element = myFixture.file.findElementAt(myFixture.caretOffset)!!.parent
    val reference = element.reference
    if (expectedUrl == null) {
      TestCase.assertNull(reference)
    }
    else {
      assertInstanceOf(reference, ExternalUrlReference::class.java)
      assertEquals(expectedUrl, (reference as ExternalUrlReference).url)

      val documentationProvider = DocumentationManager.getProviderFromElement(element)

      val url = documentationProvider.getUrlFor(reference.resolve(), element)
      TestCase.assertEquals(url, listOf(expectedUrl))

      val quickNavigateInfo = documentationProvider.getQuickNavigateInfo(reference.resolve(), element)
      TestCase.assertEquals(expectedQuickNavigateInfo, quickNavigateInfo)
    }
  }

  @Test
  fun testKeywordReferences() {

    doTest("<caret>invalid_keyword {", null)

    doTest("invalid { <caret>backend {", null)

    doTest("invalid { <caret>provisioner {", null)

    doTest("<caret>atlas {", "$BASE_URL/configuration/terraform-enterprise.html")

    doTest("<caret>data {", "$BASE_URL/configuration/data-sources.html")

    doTest("<caret>locals {", "$BASE_URL/configuration/locals.html")
        
    doTest("<caret>module {", "$BASE_URL/configuration/modules.html")

    doTest("<caret>output {", "$BASE_URL/configuration/outputs.html")

    doTest("<caret>provider {", "$BASE_URL/configuration/providers.html")

    doTest("<caret>resource {", "$BASE_URL/configuration/resources.html")

    doTest("resource \"consul_node\" { <caret>provisioner \"local-exec\" {", "$BASE_URL/provisioners/index.html")

    doTest("<caret>terraform {", "$BASE_URL/configuration/terraform.html")

    doTest("terraform { <caret>backend {", "$BASE_URL/backends/index.html")

    doTest("<caret>variable {", "$BASE_URL/configuration/variables.html")
  }

  @Test
  fun testDynamicReferences() {

    doTest("data \"<caret>consul_service\" \"foo\" {", "$BASE_URL/providers/consul/d/service.html")

    doTest("locals { \"foo\" = \"\${<caret>dirname()}\"", "$BASE_URL/configuration/interpolation.html#dirname-path-", "dirname(path) -> string")

    doTest("locals { \"foo\" = \"\${<caret>invalid_function()}\"", null)

    doTest("provider \"<caret>consul\" {", "$BASE_URL/providers/consul/")

    doTest("resource \"<caret>consul_node\" {", "$BASE_URL/providers/consul/r/node.html")

    doTest("resource \"consul_node\" { provisioner \"<caret>local-exec\" {", "$BASE_URL/provisioners/local-exec.html")

    doTest("terraform { backend \"<caret>local\" {", "$BASE_URL/backends/types/local.html")
  }
}
