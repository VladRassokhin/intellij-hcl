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
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.Nullable;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Collections;

public abstract class HCLFormatterBaseTestCase extends LightCodeInsightFixtureTestCase {
  @Parameterized.Parameter
  public LanguageFileType myFileType = HCLFileType.INSTANCE;

  @Override
  protected String getTestDataPath() {
    return "test-data/formatter";
  }


  protected void doTest(String name) throws Exception {
    doTest(name + ".input", name + ".golden");
  }

  protected void doTest(String inputFile, String expectedFile) throws Exception {
    doSimpleTest(loadFile(inputFile), loadFile(expectedFile));
  }

  protected void doSimpleTest(String input, String expected) throws Exception {
    doSimpleTest(input, expected, null);
  }

  protected void doSimpleTest(String input, String expected, @Nullable Runnable setupSettings) throws Exception {
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

  protected String loadFile(String name) throws Exception {
    String fullName = getTestDataPath() + File.separatorChar + getBasePath() + File.separatorChar + name;
    String text = FileUtil.loadFile(new File(fullName));
    text = StringUtil.convertLineSeparators(text);
    return text;
  }
}
