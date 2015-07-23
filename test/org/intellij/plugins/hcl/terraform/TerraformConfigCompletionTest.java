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
package org.intellij.plugins.hcl.terraform;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TerraformConfigCompletionTest extends LightCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(TerraformConfigCompletionTest.class);

  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  public void testBlockKeywordCompletion() throws Exception {
    doBasicCompletionTest("<caret> ", TerraformConfigCompletionProvider.BLOCK_KEYWORDS);
    doBasicCompletionTest("a=1\n<caret> ", TerraformConfigCompletionProvider.BLOCK_KEYWORDS);
  }

  public void testNoBlockKeywordCompletion() throws Exception {
    doBasicCompletionTest("a={\n<caret>\n}", 0);
    doBasicCompletionTest("resource <caret> \"aaa\" {}", 0);
    doBasicCompletionTest("resource <caret>", 0);
  }

  public void testResourceCommonPropertyCompletion() throws Exception {
    doBasicCompletionTest("resource abc {<caret>}", TerraformConfigCompletionProvider.COMMON_RESOURCE_PARAMETERS);
    final HashSet<String> set = new HashSet<String>(TerraformConfigCompletionProvider.COMMON_RESOURCE_PARAMETERS);
    set.remove("id");
    doBasicCompletionTest("resource \"x\" {\nid='a'\n<caret>\n}", set);
  }

  private void doBasicCompletionTest(String text, Set<String> expected) {
    doBasicCompletionTest(text, expected.size(), expected.toArray(new String[expected.size()]));
  }

  private void doBasicCompletionTest(String text, int expectedAllSize, String... expected) {
    final PsiFile psiFile = myFixture.configureByText("a.tf", text);
    System.out.println("PsiFile = " + DebugUtil.psiToString(psiFile, true));
    assertEquals(TerraformLanguage.INSTANCE$, psiFile.getLanguage());
    final LookupElement[] elements = myFixture.completeBasic();
    System.out.println("LookupElements = " + Arrays.toString(elements));
    final List<String> strings = myFixture.getLookupElementStrings();
    assertNotNull(strings);
    System.out.println("LookupStrings = " + strings);
    assertContainsElements(strings, expected);
    assertEquals("Actual lookup elements: " + strings, expectedAllSize, strings.size());
  }

  protected void createFile(String text) {
    PsiFile psiFile = myFixture.configureByText("text.tf", text);
    Document document = myFixture.getDocument(psiFile);
    final Project project = getProject();
  }
}
