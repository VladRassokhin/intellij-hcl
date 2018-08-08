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
import org.intellij.plugins.hcl.terraform.config.TerraformParserDefinition;

import java.io.IOException;

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

  public void testForArray() throws IOException {
    doCodeTest("a = [for k, v in foo: v if true]");
  }

  public void testForArray2() throws IOException {
    doCodeTest("cidr_blocks = [\n" +
        "  for num in var.subnet_numbers:\n" +
        "  cidrsubnet(data.aws_vpc.example.cidr_block, 8, num)\n" +
        "]");
  }

  public void testForArrayIf() throws IOException {
    doCodeTest("value = {\n" +
        "  for instance in aws_instance.example:\n" +
        "  instance.id => instance.public\n" +
        "  if instance.associate_public_ip_address\n" +
        "}");
  }

  public void testForObject() throws IOException {
    doCodeTest("value = {\n" +
        "  for instance in aws_instance.example:\n" +
        "  instance.id => instance.private_ip\n" +
        "}");
  }

  public void testForObjectGrouping() throws IOException {
    doCodeTest("value = {\n" +
        "  for instance in aws_instance.example:\n" +
        "  instance.availability_zone => instance.id...\n" +
        "}");
  }

  public void testSelectExpression() throws IOException {
    doCodeTest("a = foo.bar.baz");
  }

  public void testIndexSelectExpression() throws IOException {
    doCodeTest("a = foo[5].baz");
  }

  public void testSplatExpression() throws IOException {
    doCodeTest("a = foo.*.baz");
  }

  public void testFullSplatExpression() throws IOException {
    doCodeTest("a = foo[*].baz");
  }

  public void testComplexSplat() throws IOException {
    doCodeTest("a = tuple.*.foo.bar[0]");
  }

  public void testComplexFullSplat() throws IOException {
    doCodeTest("a = tuple[*].foo.bar[0]");
  }

  public void testPropsInObject() throws IOException {
    doCodeTest("a = {a=1, b:2, c=3\nd=4\ne:null}");
  }
}
