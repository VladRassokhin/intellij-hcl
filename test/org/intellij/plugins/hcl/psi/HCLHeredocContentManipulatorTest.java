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
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import org.assertj.core.api.BDDAssertions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "ArraysAsListWithZeroOrOneArgument"})
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

  public void testEmptyContent() throws Exception {
    final HCLHeredocContent content = content();
    final List<String> lines = content.getLines();
    assertEquals(Collections.<String>emptyList(), lines);
  }

  public void testSingleEmptyLineContent() throws Exception {
    final HCLHeredocContent content = content("");
    System.out.println(DebugUtil.psiToString(content, false, true));
    final List<String> lines = content.getLines();
    BDDAssertions.then(lines).hasSize(1).containsOnly("");
  }

  public void testContentLines() throws Exception {
    final HCLHeredocContent content = content("abc");
    System.out.println(DebugUtil.psiToString(content, false, true));
    final List<String> lines = content.getLines();
    assertEquals(Arrays.asList("abc"), lines);
  }

  public void testContentNodeTree() throws Exception {
    final HCLHeredocContent content = content("abc");
    System.out.println(DebugUtil.psiToString(content, false, true));
    assertEquals(1, content.getLinesCount());
    final List<String> lines = content.getLines();
    assertEquals(Arrays.asList("abc"), lines);
    assertEquals("abc\n", content.getValue());
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

    doTest(content("abc"), TextRange.create(0, 1), "a\n", "a", "bc");
    doTest(content("abc"), TextRange.create(1, 2), "b\n", "ab", "c");
    doTest(content("abc"), TextRange.create(2, 3), "c\n", "abc", "");

    doTest(content("abc"), TextRange.create(0, 4), "\nabc", "", "abc");
    doTest(content("abc"), TextRange.create(0, 4), "a\nbc", "a", "bc");
    doTest(content("abc"), TextRange.create(0, 4), "ab\nc", "ab", "c");
    doTest(content("abc"), TextRange.create(0, 4), "abc\n", "abc");
  }

  public void testSeparateMultipleLines() throws Exception {
    doTest(content("012", "456", "890"), TextRange.create(0, 4), "\n", "", "456", "890");
    doTest(content("012", "456", "890"), TextRange.create(4, 7), "\n", "012", "", "", "890");
    doTest(content("012", "456", "890"), TextRange.create(7, 10), "\n", "012", "456", "0");
    doTest(content("012", "456", "890"), TextRange.create(7, 11), "\n", "012", "456", "");
    doTest(content("012", "456", "890"), TextRange.create(7, 12), "\n", "012", "456", "");
  }

  public void testReplaceTextAcrossLines() throws Exception {
    doTest(content("012", "456", "890"), TextRange.create(2, 6), "x", "01x6", "890");
    doTest(content("012", "456", "890"), TextRange.create(2, 8), "x", "01x890");
    doTest(content("012", "456", "890"), TextRange.create(2, 8), "\n", "01", "890");
  }

  public void testReplaceEmptyString() throws Exception {
    doTest(content(), TextRange.create(0, 0), "text", "text");
    doTest(content(), TextRange.create(0, 0), "te\nxt", "te", "xt");
    doTestFullTextReplacement(content(), "text", content("text"));
    doTestFullTextReplacement(content(), "te\nxt", content("te", "xt"));

    doTest(content("", "", ""), TextRange.create(0, 3), "text", "text");
  }

  public void testMayBeEmptiedCompletely() throws Exception {
    doTestText(content("a\nb\nc"), TextRange.from(0, 6), "", content());
  }

  public void testReplaceInText() throws Exception {
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

  public void testReplaceFullText() throws Exception {
    doTestFullTextReplacement(content("jenkins"), "leeroy", content("leeroy"));
    doTestFullTextReplacement(content("jenkins"), "leeroy\n", content("leeroy"));
    doTestFullTextReplacement(content("a\nb"), "c", content("c"));
    doTestFullTextReplacement(content("a\nb"), "c\n", content("c"));
    doTestFullTextReplacement(content("a\nb"), "a\nb\nc\n", content("a\n", "b\n", "c\n"));
    doTestFullTextReplacement(content("a\nb"), "a\nb\nc\n", content("a", "b", "c"));
    doTestFullTextReplacement(content("a\nb"), "\n\n\n", content("", "", ""));
    doTestFullTextReplacement(content(""), "x", content("x"));
    doTestFullTextReplacement(content(""), "", content());
    doTestFullTextReplacement(content(""), "\n", content(""));
    doTestFullTextReplacement(content(), "\n", content(""));
  }

  public void testReplaceFullText2() throws Exception {
    doTestFullTextReplacement(content(false, "jenkins\n"), "leeroy\n", content(false, "leeroy\n"));
  }

  public void testReplacementLines() throws Exception {
    doReplacementLinesTest("");
    doReplacementLinesTest("\n", "", "");
    doReplacementLinesTest("a\n", "a", "");
    doReplacementLinesTest("leeroy\njenkins\n", "leeroy", "jenkins");
    doReplacementLinesTest("a", "a");
    doReplacementLinesTest("leeroy\njenkins", "leeroy", "jenkins");
  }

  private void doReplacementLinesTest(String input, String... expected) {
    final List<String> list = HCLHeredocContentManipulator.Companion.getReplacementLines(input);
    assertEquals(replaceEOLs(Arrays.asList(expected)), replaceEOLs(list));
  }

  protected List<String> replaceEOLs(List<String> list) {
    final List<String> result = new ArrayList<String>(list.size());
    for (String s : list) {
      result.add(replaceEOLs(s));
    }
    return result;
  }

  private String replaceEOLs(String s) {
    return s.replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
  }

  protected void doTest(final HCLHeredocContent content, final TextRange range, final String replacement, String... expected) {
    System.out.println(DebugUtil.psiToString(content, false, true));
    final HCLHeredocContent[] changed = new HCLHeredocContent[1];
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        changed[0] = myContentManipulator.handleContentChange(content, range, replacement);
      }
    });
    assertNotNull(changed[0]);
    System.out.println(DebugUtil.psiToString(changed[0], false, true));
    final List<String> lines = changed[0].getLines();
    assertEquals(Arrays.asList(expected), removeSuffix(lines, "\n"));
  }

  protected void doTestText(final HCLHeredocContent content, final TextRange range, final String replacement, HCLHeredocContent expected) {
    System.out.println(DebugUtil.psiToString(content, false, true));
    final HCLHeredocContent[] changed = new HCLHeredocContent[1];
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        changed[0] = myContentManipulator.handleContentChange(content, range, replacement);
      }
    });
    assertNotNull(changed[0]);
    System.out.println(DebugUtil.psiToString(changed[0], false, true));
    assertEquals(expected.getText(), changed[0].getText());
  }

  protected void doTestFullTextReplacement(final HCLHeredocContent content, final String replacement, HCLHeredocContent expected) {
    System.out.println(DebugUtil.psiToString(content, false, true));
    final HCLHeredocContent[] changed = new HCLHeredocContent[1];
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        changed[0] = myContentManipulator.handleContentChange(content, replacement);
      }
    });
    assertNotNull(changed[0]);
    System.out.println(DebugUtil.psiToString(changed[0], false, true));
    assertEquals(replaceEOLs(expected.getText()), replaceEOLs(changed[0].getText()));
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
    return content(true, lines);
  }

  protected HCLHeredocContent content(boolean appendNewLines, String... lines) {
    return myElementGenerator.createHeredocContent(Arrays.asList(lines), appendNewLines, false, 0);
  }

  protected HCLHeredocContent indentedContent(int endIndent, String... lines) {
    return indentedContent(endIndent, true, lines);
  }

  protected HCLHeredocContent indentedContent(int endIndent, boolean appendNewLines, String... lines) {
    return myElementGenerator.createHeredocContent(Arrays.asList(lines), appendNewLines, true, endIndent);
  }
}
