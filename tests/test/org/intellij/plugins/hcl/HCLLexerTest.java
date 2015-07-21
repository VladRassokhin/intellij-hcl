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

import com.intellij.lexer.Lexer;
import com.intellij.testFramework.LexerTestCase;

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
}
