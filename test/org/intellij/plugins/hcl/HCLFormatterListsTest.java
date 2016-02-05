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

public class HCLFormatterListsTest extends HCLFormatterBaseTestCase {
  public void testRemoveSpacesAfterLastElement() throws Exception {
    doSimpleTest(
        "foo = [\"fatih\", \"arslan\"           ]\n",
        "foo = [\"fatih\", \"arslan\"]\n"
    );
  }

  public void testRemoveSpacesBeforeFirstElement() throws Exception {
    doSimpleTest(
        "foo = [        \"bar\", \"qaz\", ]\n",
        "foo = [\"bar\", \"qaz\"]\n"
    );
  }

  public void testRemoveExtraSpacesBetweenElements() throws Exception {
    doSimpleTest(
        "foo = [1,   2,3,       4]\n",
        "foo = [1, 2, 3, 4]\n"
    );
  }

  public void testRemoveExtraSpacesInMultilineList() throws Exception {
    doSimpleTest(
        "foo = [             \"zeynep\", \n" +
            "\"arslan\", ]",
        "foo = [\"zeynep\",\n" +
            "  \"arslan\",\n" +
            "]"
    );
  }

  public void testEmptyList() throws Exception {
    doSimpleTest(
        "foo = []\n",
        "foo = []\n"
    );
  }

  public void testClosingBracketShouldBeOnSeparateLineInMultilineList() throws Exception {
    doSimpleTest(
            "foo = [\"fatih\", \"zeynep\",\n" +
                "\"arslan\", ]",
            "foo = [\"fatih\", \"zeynep\",\n" +
            "  \"arslan\",\n" +
            "]"
    );
  }

  public void testProperlyIndentMultilineList() throws Exception {
    doSimpleTest(
        "foo = [\n" +
            "\t\"vim-go\", \n" +
            "\t\"golang\", \"hcl\"]",
        "foo = [\n" +
            "  \"vim-go\",\n" +
            "  \"golang\",\n" +
            "  \"hcl\",\n" +
            "]"
    );
  }

  public void testOneElementPerLineInMultilineList() throws Exception {
    doSimpleTest(
        "foo = [\n" +
            "\t\"kenya\",        \"ethiopia\",\n" +
            "\t\"columbia\"]",
        "foo = [\n" +
            "  \"kenya\",\n" +
            "  \"ethiopia\",\n" +
            "  \"columbia\",\n" +
            "]"
    );
  }
}
