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
package org.intellij.plugins.hcl.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class HCLHeredocContentManipulatorTest extends LightPlatformTestCase {
  protected HCLElementGenerator myElementGenerator;
  protected HCLHeredocContentManipulator myContentManipulator;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myElementGenerator = createElementGenerator();
    myContentManipulator = new HCLHeredocContentManipulator();
  }

  @NotNull
  protected HCLElementGenerator createElementGenerator() {
    return new HCLElementGenerator(getProject());
  }

  public void testContentLines() throws Exception {
    final HCLHeredocContent content = content("abc");
    final List<String> lines = content.getLines();
    assertEquals(Arrays.asList("abc\n"), lines);
  }

  public void testKeepSame() throws Exception {
    doTest(content("abc"), TextRange.create(0, 3), "abc", "abc");
  }

  public void testReplaceSingleLineContent() throws Exception {
    doTest(content("abc"), TextRange.create(0, 1), "!", "!bc");
    doTest(content("abc"), TextRange.create(0, 2), "!", "!c");
    doTest(content("abc"), TextRange.create(0, 3), "!", "!");
    doTest(content("abc"), TextRange.create(1, 2), "!", "a!c");
    doTest(content("abc"), TextRange.create(1, 3), "!", "a!");
    doTest(content("abc"), TextRange.create(2, 3), "!", "ab!");
  }

  public void testLineBetweenOthers() throws Exception {
    doTest(content("012", "456", "890"), TextRange.create(0, 3), "!", "!", "456", "890");
    doTest(content("012", "456", "890"), TextRange.create(0, 4), "!", "!456", "890");
    doTest(content("012", "456", "890"), TextRange.create(4, 7), "!", "012", "!", "890");
    doTest(content("012", "456", "890"), TextRange.create(4, 8), "!", "012", "!890");
    doTest(content("012", "456", "890"), TextRange.create(7, 10), "!", "012", "456!0");
    doTest(content("012", "456", "890"), TextRange.create(7, 11), "!", "012", "456!");
    doTest(content("012", "456", "890"), TextRange.create(7, 12), "!", "012", "456!");
  }

  public void testSeparateSingleLine() throws Exception {
    doTest(content("abc"), TextRange.create(0, 1), "\n", "", "bc");
    doTest(content("abc"), TextRange.create(1, 2), "\n", "a", "c");
    doTest(content("abc"), TextRange.create(2, 3), "\n", "ab", "");
  }

  public void testSeparateMultipleLines() throws Exception {
    doTest(content("012", "456", "890"), TextRange.create(0, 4), "\n", "", "456", "890");
    doTest(content("012", "456", "890"), TextRange.create(4, 7), "\n", "012", "", "", "890");
    doTest(content("012", "456", "890"), TextRange.create(7, 10), "\n", "012", "456", "0");
    doTest(content("012", "456", "890"), TextRange.create(7, 11), "\n", "012", "456", "");
    doTest(content("012", "456", "890"), TextRange.create(7, 12), "\n", "012", "456");
  }

  public void testReplaceTextAcrossLines() throws Exception {
    doTest(content("012", "456", "890"), TextRange.create(2, 6), "x", "01x6", "890");
    doTest(content("012", "456", "890"), TextRange.create(2, 8), "x", "01x890");
    doTest(content("012", "456", "890"), TextRange.create(2, 8), "\n", "01", "890");
  }

  public void testReplaceFullText() throws Exception {
    doTestText(
        content("[\n" +
            "  {\n" +
            "    \"name\": \"jenkins\",\n" +
            "    \"image\": \"jenkins\",\n" +
            "    \"cpu\": 10,\n" +
            "    \"memory\": 500,\n" +
            "    \"essential\": true,\n" +
            "    \"portMappings\": [\n" +
            "      {\n" +
            "        \"containerPort\": 80,\n" +
            "        \"hostPort\": 80\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]"),
        TextRange.from(19, 7), "leeroy",
        content("[\n" +
            "  {\n" +
            "    \"name\": \"leeroy\",\n" +
            "    \"image\": \"jenkins\",\n" +
            "    \"cpu\": 10,\n" +
            "    \"memory\": 500,\n" +
            "    \"essential\": true,\n" +
            "    \"portMappings\": [\n" +
            "      {\n" +
            "        \"containerPort\": 80,\n" +
            "        \"hostPort\": 80\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]"));
  }


  protected void doTest(final HCLHeredocContent content, final TextRange range, final String replacement, String... expected) {
    final HCLHeredocContent[] changed = new HCLHeredocContent[1];
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        changed[0] = myContentManipulator.handleContentChange(content, range, replacement);
      }
    });
    assertNotNull(changed[0]);
    final List<String> lines = changed[0].getLines();
    assertEquals(Arrays.asList(expected), removeSuffix(lines, "\n"));
  }

  protected void doTestText(final HCLHeredocContent content, final TextRange range, final String replacement, HCLHeredocContent expected) {
    final HCLHeredocContent[] changed = new HCLHeredocContent[1];
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        changed[0] = myContentManipulator.handleContentChange(content, range, replacement);
      }
    });
    assertNotNull(changed[0]);
    assertEquals(expected.getText(), changed[0].getText());
  }

  private List<String> removeSuffix(List<String> input, String suffix) {
    final ArrayList<String> result = new ArrayList<String>();
    for (String s : input) {
      if (s.endsWith(suffix)) {
        s = s.substring(0, s.length() - suffix.length());
      }
      result.add(s);
    }
    return result;
  }

  protected HCLHeredocContent content(String... lines) {
    return myElementGenerator.createHeredocContent(Arrays.asList(lines));
  }
}
