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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.intellij.plugins.hcl.formatter.HCLCodeStyleSettings;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class HCLFormatterTest extends LightCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(HCLFormatterTest.class);


  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  public void testBasicFormatting() throws Exception {
    doSimpleTest("a=1", "a = 1");
    doSimpleTest("'a'=1", "'a' = 1");
  }

  public void testFormatBlock_Brace() throws Exception {
    doSimpleTest("a b c {\n  a = true}", "a b c {\n  a = true\n}");
    doSimpleTest("block x {a=true}", "block x {\n  a = true\n}");
    doSimpleTest("block x {}", "block x {\n}");
  }

  public void testFormatBlock_Space() throws Exception {
    doSimpleTest("block x{}", "block x {\n}");
    doSimpleTest("block x{a=true}", "block x {\n  a = true\n}");
  }

  public void testAlignPropertiesOnEquals() throws Exception {
    CodeStyleSettingsManager.getSettings(getProject()).getCustomSettings(HCLCodeStyleSettings.class).PROPERTY_ALIGNMENT = HCLCodeStyleSettings.ALIGN_PROPERTY_ON_EQUALS;
    doSimpleTest("a=true\nbaz=42", "a   = true\nbaz = 42");
    doSimpleTest("a = true\nbaz=42", "a   = true\nbaz = 42");
    doSimpleTest("a = true\nbaz = 42", "a   = true\nbaz = 42");
    doSimpleTest("a=true\nbaz = 42", "a   = true\nbaz = 42");
  }

  public void testAlignPropertiesOnValue() throws Exception {
    CodeStyleSettingsManager.getSettings(getProject()).getCustomSettings(HCLCodeStyleSettings.class).PROPERTY_ALIGNMENT = HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE;
    doSimpleTest("a=true\nbaz=42", "a =   true\nbaz = 42");
    doSimpleTest("a = true\nbaz=42", "a =   true\nbaz = 42");
    doSimpleTest("a = true\nbaz = 42", "a =   true\nbaz = 42");
    doSimpleTest("a=true\nbaz = 42", "a =   true\nbaz = 42");
  }

  public void doSimpleTest(String input, String expected) throws Exception {
    doSimpleTest(input, expected, null);
  }

  public void doSimpleTest(String input, String expected, @Nullable Runnable setupSettings) throws Exception {
    myFixture.configureByText(HCLFileType.INSTANCE$, input);
    final Project project = getProject();
    final PsiFile file = myFixture.getFile();
    if (setupSettings != null) setupSettings.run();
    new WriteCommandAction.Simple(project, file) {
      @Override
      public void run() {
        CodeStyleManager.getInstance(project).reformatText(file, Collections.singleton(file.getTextRange()));
      }
    }.execute().throwException();
    myFixture.checkResult(expected);
  }
}
