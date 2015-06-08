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
package org.intellij.plugins.hcl.terraform.il;

import com.intellij.testFramework.ParsingTestCase;

public class TILParserTest  extends ParsingTestCase {
  public TILParserTest() {
    super("til/psi", "til", true, new TILParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "data/";
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

  public void testOperator() throws Exception {
    doCodeTest("${count.index+1}");
  }

  public void testNoOperator() throws Exception {
    doCodeTest("${var.amis.us-east-1}");
  }

  public void testStarVariable() throws Exception {
    doCodeTest("${aws_instance.web.*.id}");
  }
}
