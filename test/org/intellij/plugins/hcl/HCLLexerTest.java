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
package org.intellij.plugins.hcl;

import com.google.common.base.Strings;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.LexerTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class HCLLexerTest extends LexerTestCase {
  @Override
  protected Lexer createLexer() {
    return new HCLLexer();
  }

  @Override
  protected String getDirPath() {
    return "data/hcl/lexer";
  }

  public void testSimple() throws Exception {
    doTest("a=1", "ID ('a')\n" +
        "= ('=')\n" +
        "NUMBER ('1')");
  }

  public void testNumberWithSuffix() throws Exception {
    doTest("a=[1k, 1Kb]", "ID ('a')\n" +
        "= ('=')\n" +
        "[ ('[')\n" +
        "NUMBER ('1')\n" +
        "ID ('k')\n" +
        ", (',')\n" +
        "WHITE_SPACE (' ')\n" +
        "NUMBER ('1')\n" +
        "ID ('Kb')\n" +
        "] (']')");
  }

  public void testStringWithCurves() throws Exception {
    doTest("a=\"{}\"", "ID ('a')\n" +
        "= ('=')\n" +
        "DOUBLE_QUOTED_STRING ('\"{}\"')");
  }

  public void testStringWith$() throws Exception {
    doTest("dollar=\"$\"", "ID ('dollar')\n" +
        "= ('=')\n" +
        "DOUBLE_QUOTED_STRING ('\"$\"')");
  }

  public void testQuotes1() throws Exception {
    doTest("a='\"1\"'", "ID ('a')\n" +
        "= ('=')\n" +
        "SINGLE_QUOTED_STRING (''\"1\"'')");
  }

  public void testQuotes2() throws Exception {
    doTest("a=\"'1'\"", "ID ('a')\n" +
        "= ('=')\n" +
        "DOUBLE_QUOTED_STRING ('\"'1'\"')");
  }

  public void testTerraformIL() throws Exception {
    doTest("count = \"${count()}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${count()}\"')");
  }

  public void testTerraformILInception() throws Exception {
    doTest("count = \"${foo(${bar()})}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${foo(${bar()})}\"')");
  }

  public void testTerraformILInception2() throws Exception {
    doTest("count = \"${${}}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${${}}\"')");
  }

  public void testTerraformILWithString() throws Exception {
    doTest("count = \"${call(\"count\")}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${call(\"')\n" +
        "ID ('count')\n" +
        "DOUBLE_QUOTED_STRING ('\")}\"')");
  }

  public void testTerraformILWithString2() throws Exception {
    doTest("count = '${call(\"count\")}'", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "SINGLE_QUOTED_STRING (''${call(\"count\")}'')");
  }

  public void testComplicatedTerraformConfigWithILStings() throws Exception {
    doTest("container_definitions = \"${file(\"ecs-container-definitions.json\")}\"", "ID ('container_definitions')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${file(\"')\n" +
        "ID ('ecs-container-definitions.json')\n" +
        "DOUBLE_QUOTED_STRING ('\")}\"')");
  }

  public void testUnfinishedString() throws Exception {
    doTest("a=\"x\"\"\n", "ID ('a')\n" +
        "= ('=')\n" +
        "DOUBLE_QUOTED_STRING ('\"x\"')\n" +
        "DOUBLE_QUOTED_STRING ('\"')\n" +
        "WHITE_SPACE ('\\n')");
  }

  public void testUnfinishedString2() throws Exception {
    doTest("a=\r\n\"x\"\"\r\n", "ID ('a')\n" +
        "= ('=')\n" +
        "WHITE_SPACE ('\n" +
        "\\n')\n" +
        "DOUBLE_QUOTED_STRING ('\"x\"')\n" +
        "DOUBLE_QUOTED_STRING ('\"')\n" +
        "WHITE_SPACE ('\n" +
        "\\n')");
  }

  public void testUnfinishedStringInObjectSingleLine() throws Exception {
    doTest("a={y = \"x\"\"}", "ID ('a')\n" +
        "= ('=')\n" +
        "{ ('{')\n" +
        "ID ('y')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"x\"')\n" +
        "DOUBLE_QUOTED_STRING ('\"}')");
  }

  public void testUnfinishedStringInObjectMultiLine() throws Exception {
    doTest("a={\ny = \"x\"\"\n}", "ID ('a')\n" +
        "= ('=')\n" +
        "{ ('{')\n" +
        "WHITE_SPACE ('\\n')\n" +
        "ID ('y')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"x\"')\n" +
        "DOUBLE_QUOTED_STRING ('\"')\n" +
        "WHITE_SPACE ('\\n')\n" +
        "} ('}')");
  }

  public void testUnfinishedInterpolation() throws Exception {
    doTest("a = \"${b(\"c\")}${{}}\"", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${b(\"')\n" +
        "ID ('c')\n" +
        "DOUBLE_QUOTED_STRING ('\")}${{}}\"')");
  }

  public void testUnfinishedInterpolation2() throws Exception {
    doTest("a = \"${b(\"c\")}${{}}\"\nx=y", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${b(\"')\n" +
        "ID ('c')\n" +
        "DOUBLE_QUOTED_STRING ('\")}${{}}\"')\n" +
        "WHITE_SPACE ('\\n')\n" +
        "ID ('x')\n" +
        "= ('=')\n" +
        "ID ('y')");
  }

  public void testHereDoc() throws Exception {
    doTest("foo = <<EOF\n" +
            "bar\n" +
            "baz\n" +
            "EOF",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "HD_MARKER ('EOF')\n" +
            "WHITE_SPACE ('\\n')\n" +
            "HD_LINE ('bar\\n')\n" +
            "HD_LINE ('baz\\n')\n" +
            "HD_MARKER ('EOF')\n");
  }

  public void testHereDoc2() throws Exception {
    doTest("foo = <<EOF\n" +
            "bar\n" +
            "baz\n" +
            "EOF\n",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "HD_MARKER ('EOF')\n" +
            "WHITE_SPACE ('\\n')\n" +
            "HD_LINE ('bar\\n')\n" +
            "HD_LINE ('baz\\n')\n" +
            "HD_MARKER ('EOF')\n" +
            "WHITE_SPACE ('\\n')");
  }

  public void testHereDoc_Indented() throws Exception {
    doTest("foo = <<-EOF\n" +
            "  bar\n" +
            "  baz\n" +
            "  EOF\n",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "HD_MARKER ('-EOF')\n" +
            "WHITE_SPACE ('\\n')\n" +
            "HD_LINE ('  bar\\n')\n" +
            "HD_LINE ('  baz\\n')\n" +
            "HD_MARKER ('  EOF')\n" +
            "WHITE_SPACE ('\\n')");
  }

  public void testHereDoc_Indented_End() throws Exception {
    doTest("foo = <<EOF\n" +
            "  bar\n" +
            "  baz\n" +
            "  EOF\n",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "HD_MARKER ('EOF')\n" +
            "WHITE_SPACE ('\\n')\n" +
            "HD_LINE ('  bar\\n')\n" +
            "HD_LINE ('  baz\\n')\n" +
            "HD_MARKER ('  EOF')\n" +
            "WHITE_SPACE ('\\n')");
  }

  public void testHereDoc_Empty() throws Exception {
    doTest("foo = <<EOF\n" +
            "EOF",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "HD_MARKER ('EOF')\n" +
            "WHITE_SPACE ('\\n')\n" +
            "HD_MARKER ('EOF')\n");
  }

  public void testHereDoc_Incomplete() throws Exception {
    doTest("foo = <<EOF\n" +
            "bar\n",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "HD_MARKER ('EOF')\n" +
            "WHITE_SPACE ('\\n')\n" +
            "HD_LINE ('bar\\n')\n" +
            "BAD_CHARACTER ('')");
  }

  public void testHereDoc_IncompleteStart() throws Exception {
    doTest("foo = <<\n" +
            "bar\n",
        "ID ('foo')\n" +
            "WHITE_SPACE (' ')\n" +
            "= ('=')\n" +
            "WHITE_SPACE (' ')\n" +
            "HD_START ('<<')\n" +
            "BAD_CHARACTER ('\\n')\n" +
            "ID ('bar')\n" +
            "WHITE_SPACE ('\\n')");
  }

  protected final static String f100 = Strings.repeat("f", 100);

  protected void doSimpleTokenTest(@NotNull IElementType expected, @NotNull String text) {
    final Lexer lexer = createLexer();
    lexer.start(text, 0, text.length());
    final IElementType first = lexer.getTokenType();
    assertNotNull(first);
    assertEquals(0, lexer.getState());

    lexer.advance();
    assertNull("Should be only one token in: " + text + "\nSecond is " + lexer.getTokenType() + "(" + lexer.getTokenText() + ")", lexer.getTokenType());

    assertEquals(0, lexer.getState());
    assertEquals(expected, first);
  }

  // testSimpleTokens_* methods uses inputs from hcl/scanner/scanner_test.go#tokenLists

  public void testSimpleTokens_Comment() throws Exception {
    List<String> line_comments = Arrays.asList(
        "//",
        "////",
        "// comment",
        "// /* comment */",
        "// // comment //",
        "//" + f100,
        "#",
        "##",
        "# comment",
        "# /* comment */",
        "# # comment #",
        "#" + f100
    );
    List<String> block_comments = Arrays.asList(
        "/**/",
        "/***/",
        "/* comment */",
        "/* // comment */",
        "/* /* comment */",
        "/*\n comment\n*/",
        "/*" + f100 + "*/"
    );
    for (String comment : line_comments) {
      doSimpleTokenTest(HCLElementTypes.LINE_COMMENT, comment);
    }
    for (String comment : block_comments) {
      doSimpleTokenTest(HCLElementTypes.BLOCK_COMMENT, comment);
    }
  }

  public void testSimpleTokens_Boolean() throws Exception {
    doSimpleTokenTest(HCLElementTypes.TRUE, "true");
    doSimpleTokenTest(HCLElementTypes.FALSE, "false");
  }

  public void testSimpleTokens_Identifier() throws Exception {
    List<String> identifiers = Arrays.asList(
        "a",
        "a0",
        "foobar",
        "foo-bar",
        "abc123",
        "LGTM",
        "_",
        "_abc123",
        "abc123_",
        "_abc_123",
        "_äöü",
        "_本",
        "äöü",
        "本",
        "a۰۱۸",
        "foo६४",
        "bar９８７６",
        "_0_"
    );
    for (String input : identifiers) {
      doSimpleTokenTest(HCLElementTypes.ID, input);
    }
  }

  public void testSimpleTokens_String() throws Exception {
    List<String> strings = Arrays.asList(
        "\" \"",
        "\"a\"",
        "\"本\"",
        "\"\\a\"",
        "\"\\b\"",
        "\"\\f\"",
        "\"\\n\"",
        "\"\\r\"",
        "\"\\t\"",
        "\"\\v\"",
        "\"\\\"\"",
        "\"\\000\"",
        "\"\\777\"",
        "\"\\x00\"",
        "\"\\xff\"",
        "\"\\u0000\"",
        "\"\\ufA16\"",
        "\"\\U00000000\"",
        "\"\\U0000ffAB\"",
        "\"" + f100 + "\""
    );
    for (String input : strings) {
      doSimpleTokenTest(HCLElementTypes.DOUBLE_QUOTED_STRING, input);
    }
  }

  public void testSimpleTokens_Number() throws Exception {
    List<String> numbers = Arrays.asList(
        "0",
        "1",
        "9",
        "42",
        "1234567890",
        "00",
        "01",
        "07",
        "042",
        "01234567",
        "0x0",
        "0x1",
        "0xf",
        "0x42",
        "0x123456789abcDEF",
        "0x" + f100,
        "0X0",
        "0X1",
        "0XF",
        "0X42",
        "0X123456789abcDEF",
        "0X" + f100,
        "-0",
        "-1",
        "-9",
        "-42",
        "-1234567890",
        "-00",
        "-01",
        "-07",
        "-29",
        "-042",
        "-01234567",
        "-0x0",
        "-0x1",
        "-0xf",
        "-0x42",
        "-0x123456789abcDEF",
        "-0x" + f100,
        "-0X0",
        "-0X1",
        "-0XF",
        "-0X42",
        "-0X123456789abcDEF",
        "-0X" + f100,
        "0"
    );
    for (String input : numbers) {
      doSimpleTokenTest(HCLElementTypes.NUMBER, input);
    }
  }

  public void testSimpleTokens_Float() throws Exception {
    List<String> floats = Arrays.asList(
        "0.",
        "1.",
        "42.",
        "01234567890.",
        ".0",
        ".1",
        ".42",
        ".0123456789",
        "0.0",
        "1.0",
        "42.0",
        "01234567890.0",
        "0e0",
        "1e0",
        "42e0",
        "01234567890e0",
        "0E0",
        "1E0",
        "42E0",
        "01234567890E0",
        "0e+10",
        "1e-10",
        "42e+10",
        "01234567890e-10",
        "0E+10",
        "1E-10",
        "42E+10",
        "01234567890E-10",
        "01.8e0",
        "1.4e0",
        "42.2e0",
        "01234567890.12e0",
        "0.E0",
        "1.12E0",
        "42.123E0",
        "01234567890.213E0",
        "0.2e+10",
        "1.2e-10",
        "42.54e+10",
        "01234567890.98e-10",
        "0.1E+10",
        "1.1E-10",
        "42.1E+10",
        "01234567890.1E-10",
        "-0.0",
        "-1.0",
        "-42.0",
        "-01234567890.0",
        "-0e0",
        "-1e0",
        "-42e0",
        "-01234567890e0",
        "-0E0",
        "-1E0",
        "-42E0",
        "-01234567890E0",
        "-0e+10",
        "-1e-10",
        "-42e+10",
        "-01234567890e-10",
        "-0E+10",
        "-1E-10",
        "-42E+10",
        "-01234567890E-10",
        "-01.8e0",
        "-1.4e0",
        "-42.2e0",
        "-01234567890.12e0",
        "-0.E0",
        "-1.12E0",
        "-42.123E0",
        "-01234567890.213E0",
        "-0.2e+10",
        "-1.2e-10",
        "-42.54e+10",
        "-01234567890.98e-10",
        "-0.1E+10",
        "-1.1E-10",
        "-42.1E+10",
        "-01234567890.1E-10"
    );
    for (String input : floats) {
      doSimpleTokenTest(HCLElementTypes.NUMBER, input);
    }
  }
}
