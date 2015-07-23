/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.il;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage;

import java.util.Arrays;
import java.util.List;

public class TILCompletionTest extends LightCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(TILCompletionTest.class);

  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  public void testMethodCompletion() throws Exception {
    final PsiFile psiFile = myFixture.configureByText("a.tf", "a='${<caret>}'");
    assertEquals(TILLanguage.INSTANCE$, psiFile.getLanguage());
    System.out.println("PsiFile = " + DebugUtil.psiToString(psiFile, true));
//    myFixture.configureByFiles("CompleteTestData.java", "DefaultTestData.simple");
    final LookupElement[] elements = myFixture.complete(CompletionType.BASIC, 1);
    System.out.println("LookupElements = " + Arrays.toString(elements));
    final List<String> strings = myFixture.getLookupElementStrings();
    assertNotNull(strings);
    System.out.println("LookupStrings = " + strings);
    assertContainsElements(strings, "concat", "file");
    assertEquals(12, strings.size());
  }
}
