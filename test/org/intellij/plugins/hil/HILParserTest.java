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
package org.intellij.plugins.hil;

import com.intellij.testFramework.ParsingTestCase;

public class HILParserTest extends ParsingTestCase {
  public HILParserTest() {
    super("hil/psi", "hil", true, new HILParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "test-data/";
  }

  private void doTest() {
    doTest(true);
  }

  public void testSimple() throws Exception {
    doCodeTest("${variable}");
  }

  public void testSimpleNoHolder() throws Exception {
    doCodeTest("variable");
  }

  public void testMethodCall() throws Exception {
    doCodeTest("method(a,v)");
  }

  public void testLongMethodCall() throws Exception {
    doCodeTest("foo.bar.baz.method(a,v)");
  }

  public void testOperator() throws Exception {
    doCodeTest("${count.index+1}");
  }

  public void testNoOperator() throws Exception {
    doCodeTest("${var.amis.us-east-1}");
  }

  public void testStarVariable() throws Exception {
    doCodeTest("${aws_instance.web.*.id}");
  }

  public void testStarQuotedVariable() throws Exception {
    doCodeTest("${aws_instance.web.\"*\".id}");
  }

  public void testSelectFromNumber() throws Exception {
    doCodeTest("${aws_instance.web.10.id}");
  }

  public void testInception() throws Exception {
    doCodeTest("${aws_instance.web.${count.index}.id}");
  }

  public void testString() throws Exception {
    doCodeTest("${file(\"ecs-container-definitions.json\")}");
  }

  public void testUnaryNumbers() throws Exception {
    doCodeTest("${format(\"\", 0, 1, -1, +1, -0, +0, -10.0e5, +10.5e-2)}");
  }

  public void testUnaryMathExpressions() throws Exception {
    doCodeTest("${format(\"\", +10 - 9, -10 + -9, -10 + (-9), -1 * -1)}");
  }

  public void testSimpleMath() throws Exception {
    doCodeTest("${format(\"\", 2 + 2, 2 - 2, 2 * 2, 2 / 2, 2 % 2)}");
  }

  public void testSimpleMathCompact() throws Exception {
    doCodeTest("${format(\"\", 2+2, 2-2, 2*2, 2/2, 2%2)}");
  }

  public void testOrderOfMathOperations() throws Exception {
    doCodeTest("${format(\"\", 2 + 2 * 2, 2 + (2 * 2))}");
  }

  public void testSimpleIndexes() throws Exception {
    doCodeTest("${format(foo[1], baz[0])}");
  }

  public void testGreedyIndexes() throws Exception {
    doCodeTest("${aws_instance.web.*.list[2]}");
  }

  public void testInceptionIndexes() throws Exception {
    doCodeTest("${foo[bar[0]]}");
  }

  public void testMultipleIndexes() throws Exception {
    doCodeTest("${foo[0][1][2][3]}");
  }

  public void testTooManyIndexes() throws Exception {
    doCodeTest("${foo[a[0]][b[1]][c[2]][d[3]]}");
  }

  public void testSlashesEscaping() throws Exception {
    doCodeTest("${join(\"\\\",\\\"\", values(var.developers))}");
  }

  public void testTernaryOp() throws Exception {
    doCodeTest("${true ? 1 : 2}");
  }

  public void testTernaryComplexOp() throws Exception {
    doCodeTest("${a < 5 ? a + 5 : !false && true}");
  }

  public void testLogicalOps() throws Exception {
    doCodeTest("${true || !false && true}");
  }

  public void testCompareOps() throws Exception {
    doCodeTest("${format(\"\", 1 < 2, 1 > 2, 1 <= 2, 1 >= 2, 1 == 2, 1 != 2)}");
  }

  public void testOrderOfBinaryOperations() throws Exception {
    doCodeTest("${format(\"\", a<5||b>2, a<5&&b>2, a<5 != b>2, a+1 != b+2)}");
  }

}
