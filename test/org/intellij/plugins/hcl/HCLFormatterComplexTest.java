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

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.intellij.plugins.hcl.terraform.config.TerraformFileType;

public class HCLFormatterComplexTest extends HCLFormatterBaseTestCase {

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

  public void test_comment() throws Exception {
    doTest("comment");
  }

  public void test_comment_aligned() throws Exception {
    doTest("comment_aligned");
  }

  public void test_comment_array() throws Exception {
    doTest("comment_array");
  }

  public void test_comment_end_file() throws Exception {
    doTest("comment_end_file");
  }

  public void test_comment_multiline_indent() throws Exception {
    doTest("comment_multiline_indent");
  }

  public void test_comment_multiline_no_stanza() throws Exception {
    doTest("comment_multiline_no_stanza");
  }

  public void test_comment_multiline_stanza() throws Exception {
    doTest("comment_multiline_stanza");
  }

  public void test_comment_newline() throws Exception {
    doTest("comment_newline");
  }

  public void test_comment_object_multi() throws Exception {
    doTest("comment_object_multi");
  }

  public void test_comment_standalone() throws Exception {
    doTest("comment_standalone");
  }

  public void test_complexhcl() throws Exception {
    doTest("complexhcl");
  }

  public void test_empty_block() throws Exception {
    doTest("empty_block");
  }

  public void test_list() throws Exception {
    doTest("list");
  }

  public void test_list_comment() throws Exception {
    doTest("list_comment");
  }

  public void test_list_of_objects() throws Exception {
    doTest("list_of_objects");
  }

  public void test_multiline_string() throws Exception {
    LanguageFileType type = myFileType;
    try {
      myFileType = TerraformFileType.INSTANCE;
      doTest("multiline_string");
    } finally {
      myFileType = type;
    }
  }

  public void test_object_singleline() throws Exception {
    doTest("object_singleline");
  }

  public void test_object_with_heredoc() throws Exception {
    doTest("object_with_heredoc");
  }

}
