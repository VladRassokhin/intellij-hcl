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

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import org.intellij.plugins.hcl.CompletionTestCase;
import org.intellij.plugins.hcl.terraform.il.codeinsight.TILCompletionProvider;

public class TILCompletionTest extends CompletionTestCase {
  private static final Logger LOG = Logger.getInstance(TILCompletionTest.class);

  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  @Override
  protected String getFileName() {
    return "a.tf";
  }

  @Override
  protected Language getExpectedLanguage() {
    return TILLanguage.INSTANCE;
  }

  public void testMethodCompletion_BeginOnInterpolation() throws Exception {
    doBasicCompletionTest("a='${<caret>}'", TILCompletionProvider.TERRAFORM_METHODS);
  }

  public void testMethodCompletion_AsParameter() throws Exception {
    doBasicCompletionTest("a='${foo(<caret>)}'", TILCompletionProvider.TERRAFORM_METHODS);
    doBasicCompletionTest("a='${foo(true,<caret>)}'", TILCompletionProvider.TERRAFORM_METHODS);
  }

  public void testNoMethodCompletion_InSelect() throws Exception {
    doBasicCompletionTest("a='${foo.<caret>}'", 0);
  }

  public void testSimpleVariableCompletion() throws Exception {
    doBasicCompletionTest("a='${var.<caret>}'", 0);
    doBasicCompletionTest("variable 'x' {}\na='${var.<caret>}'", 1, "x");
    doBasicCompletionTest("variable 'x' {default={a=true b=false}}\nfoo='${var.<caret>}'", 1, "x");
    doBasicCompletionTest("variable 'x' {}\nvariable 'y' {}\na='${var.<caret>}'", 2, "x", "y");
    doBasicCompletionTest("variable 'x' {}\nvariable 'y' {}\na='${concat(var.<caret>)}'", 2, "x", "y");
  }

  public void testMappingVariableCompletion() throws Exception {
    doBasicCompletionTest("variable 'x' {default={a=true b=false}}\nfoo='${var.x.<caret>}'", 2, "a", "b");
  }
}
