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

public class HCLFormatterComplexImpl extends HCLFormatterBaseTestCase {
  // Next 3 tests are based on test data from https://github.com/hashicorp/hcl/blob/master/hcl/fmtcmd/fmtcmd_test.go
  public void testNoop() throws Exception {
    doSimpleTest("resource \"aws_security_group\" \"firewall\" {\n" +
            "  count = 5\n" +
            "}",
        "resource \"aws_security_group\" \"firewall\" {\n" +
            "  count = 5\n" +
            "}");
  }

  public void testAlignEqualsByDefault() throws Exception {
    doSimpleTest("variable \"foo\" {\n" +
            "  default = \"bar\"\n" +
            "  description = \"bar\"\n" +
            "}",
        "variable \"foo\" {\n" +
            "  default     = \"bar\"\n" +
            "  description = \"bar\"\n" +
            "}");
  }

  public void testIndentation() throws Exception {
    doSimpleTest("provider \"aws\" {\n" +
            "    access_key = \"foo\"\n" +
            "    secret_key = \"bar\"\n" +
            "}",
        "provider \"aws\" {\n" +
            "  access_key = \"foo\"\n" +
            "  secret_key = \"bar\"\n" +
            "}");
  }

  // Next tests based on https://github.com/hashicorp/hcl/blob/master/hcl/printer/printer_test.go

  public void testComment() throws Exception {
    doTest("comment");
  }

  public void testComment_Aligned() throws Exception {
    doTest("comment_aligned");
  }

  public void testComment_Standalone() throws Exception {
    doTest("comment_standalone");
  }

  public void testComplexHCL() throws Exception {
    doTest("complexhcl");
  }

  public void testEmptyBlock() throws Exception {
    doTest("empty_block");
  }

  public void testList() throws Exception {
    doTest("list");
  }

  public void testListOfObjects() throws Exception {
    doTest("list_of_objects");
  }

}
