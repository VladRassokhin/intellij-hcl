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

import com.intellij.lexer.Lexer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class TerraformConfigLexerTest extends HCLLexerTest {
  @Override
  protected Lexer createLexer() {
    return new HCLLexer(EnumSet.allOf(HCLCapability.class));
  }

  public void testNumberWithSuffix() {
    doTest("arr=[1k, 1Kb]", "ID ('arr')\n" +
        "= ('=')\n" +
        "[ ('[')\n" +
        "NUMBER ('1k')\n" +
        ", (',')\n" +
        "WHITE_SPACE (' ')\n" +
        "NUMBER ('1Kb')\n" +
        "] (']')");
  }

  public void testTerraformIL() {
    doTest("count = \"${count()}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${count()}\"')");
  }

  public void testTerraformILWithSpecials() {
    doTest("a = \"${$()}\"", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${$()}\"')");
  }

  public void testTerraformILWithSpecials2() {
    doTest("a = \"${{$}$}}\"", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${{$}$}}\"')");
  }

  public void testTerraformILInception() {
    doTest("count = \"${foo(${bar()})}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${foo(${bar()})}\"')");
  }

  public void testTerraformILInception2() {
    doTest("count = \"${${}}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${${}}\"')");
  }

  public void testTerraformILWithString() {
    doTest("count = \"${call(\"count\")}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${call(\"count\")}\"')");
  }

  public void testTerraformILWithIncorrectString() {
    doTest("count = \"${call(incomplete\")}\"", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${call(incomplete\")}\"')");
  }

  public void testTerraformILWithString2() {
    doTest("count = '${call(\"count\")}'", "ID ('count')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "SINGLE_QUOTED_STRING (''${call(\"count\")}'')");
  }

  public void testTerraformILWithStringWithClosingBrace() {
    doTest("a = \"${foo(\"}\")}\"", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${foo(\"}\")}\"')");
  }

  public void testTerraformILWithString_Unfinished() {
    doTest("a = '${\"uf)}'", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "SINGLE_QUOTED_STRING (''${\"uf)}'')");
  }

  public void testTerraformILWithString_Unfinished2() {
    doTest("a = \"${\"uf)}\"", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${\"uf)}\"')");
  }

  public void testTerraformILWithString_Unfinished3() {
    doTest("c{a = \"${f(\"b.json\")}\"\'}", "ID ('c')\n" +
        "{ ('{')\n" +
        "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${f(\"b.json\")}\"')\n" +
        "SINGLE_QUOTED_STRING (''}')");
  }

  public void testComplicatedTerraformConfigWithILStings() {
    doTest("container_definitions = \"${file(\"ecs-container-definitions.json\")}\"", "ID ('container_definitions')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${file(\"ecs-container-definitions.json\")}\"')");
  }

  public void testUnfinishedInterpolation() {
    doTest("a = \"${b(\"c\")}${{}}\"", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${b(\"c\")}${{}}\"')");
  }

  public void testUnfinishedInterpolation2() {
    doTest("a = \"${b(\"c\")}${\"\nx=y", "ID ('a')\n" +
        "WHITE_SPACE (' ')\n" +
        "= ('=')\n" +
        "WHITE_SPACE (' ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${b(\"c\")}${\"\\nx=y')");
  }

  public void testSimpleTokens_String_With_Interpolation() {
    List<String> strings = Arrays.asList(
        "\"${file(\"foo\")}\"",
        "\"${file(\\\"foo\\\")}\"",
        "\"${file(\\\"" + f100 + "\\\")}\"",
        "\"${join(\"\\\\\",\\\\\"\", values(var.developers))}\""
    );
    for (String input : strings) {
      doSimpleTokenTest(HCLElementTypes.DOUBLE_QUOTED_STRING, input);
    }
  }

  public void testSimpleTokens_Number_With_Modifier() {
    List<String> strings = Arrays.asList(
        "1k",
        "1Kb"
    );
    for (String input : strings) {
      doSimpleTokenTest(HCLElementTypes.NUMBER, input);
    }
  }

  public void testMultilineString_WithInterpolation() {
    doTest("mli=\"${hello\n  world}\"", "ID ('mli')\n" +
        "= ('=')\n" +
        "DOUBLE_QUOTED_STRING ('\"${hello\\n  world}\"')\n");
  }

  public void testNonEscapedQuoteInInterpolation() {
    doTest("x=[\n" +
        " \"${\"a\"}\",\n" +
        " \"${\"b\\\\\"}\\\\\",\n" +
        "]", "ID ('x')\n" +
        "= ('=')\n" +
        "[ ('[')\n" +
        "WHITE_SPACE ('\\n ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${\"a\"}\"')\n" +
        ", (',')\n" +
        "WHITE_SPACE ('\\n ')\n" +
        "DOUBLE_QUOTED_STRING ('\"${\"b\\\\\"}\\\\\"')\n" +
        ", (',')\n" +
        "WHITE_SPACE ('\\n')\n" +
        "] (']')" +
        "");
  }

  public void testEscapedQuoteInInterpolation() {
    doTest("\"${\"\\\"x\\\"\"}\"\n",
        "DOUBLE_QUOTED_STRING ('\"${\"\\\"x\\\"\"}\"')\n" +
        "WHITE_SPACE ('\\n')\n");
  }
}
