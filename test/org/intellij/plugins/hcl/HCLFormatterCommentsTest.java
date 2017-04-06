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

public class HCLFormatterCommentsTest extends HCLFormatterBaseTestCase {
  public void testListElementsAlignedComments() throws Exception {
    doSimpleTest(
        "security_groups = [\n" +
            "\"foo\",# kenya 1\n" +
            "\"${aws_security_group.firewall.foo}\", # kenya 2\n" +
            "]",
        "security_groups = [\n" +
            "  \"foo\",                                # kenya 1\n" +
            "  \"${aws_security_group.firewall.foo}\", # kenya 2\n" +
            "]"
    );
  }

  public void testListElementsStandaloneComments() throws Exception {
    doSimpleTest(
        "a = [\n" +
            "# c1\n" +
            "\"foo\",\n" +
            "# c2\n" +
            "\"barz\"\n" +
            "# c3\n" +
            "]",
        "a = [\n" +
            "  # c1\n" +
            "  \"foo\",\n" +
            "  # c2\n" +
            "  \"barz\",\n" +
            "  # c3\n" +
            "]");
  }
}
