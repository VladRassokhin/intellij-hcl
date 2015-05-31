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

import com.intellij.testFramework.ParsingTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/data/psi/")
public class HCLParserTest extends ParsingTestCase {
  public HCLParserTest() {
    super("psi", "hcl", true, new HCLParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "data/";
  }

  private void doTest() {
    doTest(true);
  }


  public void testEmpty() throws Exception {
    doTest();
  }

  public void testComment_Single() throws Exception {
    doTest();
  }

  public void testComment_Multiline() throws Exception {
    doTest();
  }

  public void testComment_Complex() throws Exception {
    doTest();
  }

  public void testSimple_Types() throws Exception {
    doTest();
  }

  public void testList_Simple() throws Exception {
    doTest();
  }

  public void testList_Tailing_Comma() throws Exception {
    doTest();
  }

  public void testMultiple_Properties() throws Exception {
    doTest();
  }

  public void testIdentifiers() throws Exception {
    doTest();
  }

  public void testBlock_Empty() throws Exception {
    doTest();
  }

  public void testBlock() throws Exception {
    doTest();
  }

  public void testComplex() throws Exception {
    doTest();
  }

}
