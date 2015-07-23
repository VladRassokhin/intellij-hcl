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
package org.intellij.plugins.hcl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class HCLFormatterTest extends LightCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(HCLFormatterTest.class);


  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  public void testFormatter() throws Exception {
//    doTest("", "");
    doTest("a=1\n", "a = 1");
    doTest("'a'=1", "'a' = 1");
  }

  public void doTest(String input, String expected) throws Exception {
    myFixture.configureByText(HCLFileType.INSTANCE$, input);
    CodeStyleSettingsManager.getSettings(getProject()).SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile(), true);
      }
    });
    myFixture.checkResult(expected);
  }
}
