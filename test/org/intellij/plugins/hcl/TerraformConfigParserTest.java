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

import com.intellij.testFramework.TestDataPath;
import org.intellij.plugins.TestFor;
import org.intellij.plugins.hcl.terraform.config.TerraformParserDefinition;

@TestDataPath("$CONTENT_ROOT/test-data/psi/")
public class TerraformConfigParserTest extends HCLParserTest {
  public TerraformConfigParserTest() {
    super("psi", "hcl", false, new TerraformParserDefinition(), new HCLParserDefinition());
  }

  private void setTerraformExtension() {
    myFileExt = "tf";
  }

  public void testTerraform_With_String_In_IL() throws Exception {
    setTerraformExtension();
    doTest();
  }

  public void testTerraform_With_Extra_Quote() throws Exception {
    setTerraformExtension();
    doTest();
  }

  public void testUnfinished_Interpolation() throws Exception {
    setTerraformExtension();
    doTest();
  }

  public void testBackslash_Escaping_In_Interpolation() throws Exception {
    setTerraformExtension();
    doTest();
  }

  public void testMultiline_Interpolation() throws Exception {
    setTerraformExtension();
    doTest();
  }

  public void testClosingBraceInInterpolationStringLiteral() throws Exception {
    setTerraformExtension();
    doTest();
  }

  public void testEscapedQuotesInInterpolation() {
    setTerraformExtension();
    doTest();
  }
}
