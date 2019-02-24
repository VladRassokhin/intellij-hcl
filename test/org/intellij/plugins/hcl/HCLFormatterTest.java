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
package org.intellij.plugins.hcl;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.intellij.plugins.hcl.formatter.HCLCodeStyleSettings;
import org.intellij.plugins.hcl.terraform.config.TerraformFileType;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@RunWith(Parameterized.class)
public class HCLFormatterTest extends LightPlatformCodeInsightFixtureTestCase {

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {HCLFileType.INSTANCE}, {TerraformFileType.INSTANCE}
    });
  }

  @SuppressWarnings("WeakerAccess")
  @Parameterized.Parameter
  public LanguageFileType myFileType = TerraformFileType.INSTANCE;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  @Test
  public void testBasicFormatting() throws Exception {
    doSimpleTest("a=1", "a = 1");
    doSimpleTest("'a'=1", "'a' = 1");
  }

  @Test
  public void testFormatBlock_Brace() throws Exception {
    doSimpleTest("a b c {\n  a = true}", "a b c {\n  a = true\n}");
    doSimpleTest("block x {a=true}", "block x {\n  a = true\n}");
    doSimpleTest("block x {}", "block x {}");
    doSimpleTest("block x {\n}", "block x {\n}");
  }

  @Test
  public void testFormatBlock_Space() throws Exception {
    doSimpleTest("block x{}", "block x {}");
    doSimpleTest("block 'x'{}", "block 'x' {}");
    doSimpleTest("block 'x'{\n}", "block 'x' {\n}");
    doSimpleTest("block x{a=true}", "block x {\n  a = true\n}");
  }

  @Test
  public void testAlignPropertiesOnEquals() throws Exception {
    CodeStyleSettingsManager.getSettings(getProject()).getCustomSettings(HCLCodeStyleSettings.class).PROPERTY_ALIGNMENT = HCLCodeStyleSettings.ALIGN_PROPERTY_ON_EQUALS;
    doSimpleTest("a=true\nbaz=42", "a   = true\nbaz = 42");
    doSimpleTest("a = true\nbaz=42", "a   = true\nbaz = 42");
    doSimpleTest("a = true\nbaz = 42", "a   = true\nbaz = 42");
    doSimpleTest("a=true\nbaz = 42", "a   = true\nbaz = 42");
  }

  @Test
  public void testAlignPropertiesOnValue() throws Exception {
    CodeStyleSettingsManager.getSettings(getProject()).getCustomSettings(HCLCodeStyleSettings.class).PROPERTY_ALIGNMENT = HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE;
    doSimpleTest("a=true\nbaz=42", "a =   true\nbaz = 42");
    doSimpleTest("a = true\nbaz=42", "a =   true\nbaz = 42");
    doSimpleTest("a = true\nbaz = 42", "a =   true\nbaz = 42");
    doSimpleTest("a=true\nbaz = 42", "a =   true\nbaz = 42");
  }

  @Test
  public void testFormatHeredoc() throws Exception {
    doSimpleTest("a=<<E\nE", "a = <<E\nE");
    doSimpleTest("a=<<E\n  E", "a = <<E\n  E");
    doSimpleTest("a=<<E\n\tE", "a = <<E\n\tE");
    doSimpleTest("a=<<E\n inner\nE", "a = <<E\n inner\nE");
    doSimpleTest("a=<<E\n inner\n  E", "a = <<E\n inner\n  E");
    doSimpleTest("a=<<E\n inner\n\tE", "a = <<E\n inner\n\tE");
  }

  @Test
  public void testFormatAfterHeredoc() throws Exception {
    doSimpleTest("a_local = [\n" +
        "  <<DATA\n" +
        "This is some data string\n" +
        "DATA\n" +
        ",\n" +
        "  \"some other data\",\n" +
        "]", "a_local = [\n" +
        "  <<DATA\n" +
        "This is some data string\n" +
        "DATA\n" +
        ",\n" +
        "  \"some other data\",\n" +
        "]");
    doSimpleTest("a_local = [\n" +
        "  <<DATA\n" +
        "This is some data string\n" +
        "DATA\n" +
        "," +
        "  \"some other data\",\n" +
        "]", "a_local = [\n" +
        "  <<DATA\n" +
        "This is some data string\n" +
        "DATA\n" +
        ",\n" +
        "  \"some other data\",\n" +
        "]");
  }

  @Test
  public void testAlignPropertiesOnValueAndSplitByBlocks() throws Exception {
    CodeStyleSettingsManager.getSettings(getProject()).getCustomSettings(HCLCodeStyleSettings.class).PROPERTY_ALIGNMENT = HCLCodeStyleSettings.ALIGN_PROPERTY_ON_VALUE;
    doSimpleTest("a=true\n\n\nbaz=42", "a = true\n\n\nbaz = 42");
  }

  public void doSimpleTest(String input, String expected) throws Exception {
    doSimpleTest(input, expected, null);
  }

  public void doSimpleTest(String input, String expected, @Nullable Runnable setupSettings) throws Exception {
    myFixture.configureByText(myFileType, input);
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
