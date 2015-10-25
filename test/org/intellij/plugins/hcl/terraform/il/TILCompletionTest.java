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
import org.intellij.plugins.hcl.CompletionTestCase;
import org.intellij.plugins.hcl.terraform.il.codeinsight.TILCompletionContributor;

public class TILCompletionTest extends CompletionTestCase {
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
    doBasicCompletionTest("a='${<caret>}'", getPartialMatcher(TILCompletionContributor.GLOBAL_AVAILABLE));
  }

  public void testMethodCompletion_AsParameter() throws Exception {
    doBasicCompletionTest("a='${foo(<caret>)}'", getPartialMatcher(TILCompletionContributor.GLOBAL_AVAILABLE));
    doBasicCompletionTest("a='${foo(true,<caret>)}'", getPartialMatcher(TILCompletionContributor.GLOBAL_AVAILABLE));
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

  public void testSelfReferenceCompletion() throws Exception {
    doBasicCompletionTest("resource 'aws_instance' 'x' {provisioner 'file' {file = '${self.<caret>}}'", "ami", "instance_type");
    doBasicCompletionTest("resource 'abracadabra' 'x' {provisioner 'file' {file = '${self.<caret>}}'", "count");
    doBasicCompletionTest("resource 'abracadabra' 'x' {file = '${self.<caret>}'", 0);
  }

  public void testPathCompletion() throws Exception {
    doBasicCompletionTest("a='${path.<caret>}'", "cwd", "module", "root");
  }

  public void testCountCompletion() throws Exception {
    doBasicCompletionTest("resource 'y' 'x' {count = 2 source='${count.<caret>}'", 1, "index");
    doBasicCompletionTest("resource 'y' 'x' {source='${count.<caret>}'", 1, "index");
    doBasicCompletionTest("resource 'y' 'x' {count = 2 source='${count.<caret> + 1}'", 1, "index");
  }

  public void testModuleCompletion() throws Exception {
    doBasicCompletionTest("module 'ref' {source = './child'} foo='${module.<caret>}'", 1, "ref");
  }

  public void testResourceTypeCompletion() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${<caret>}'", "res_a");
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${concat(<caret>)}'", "res_a");
  }

  public void testResourceNameCompletion() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${res_a.<caret>}'", "b");
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${concat(res_a.<caret>)}'", "b");
  }

  public void testResourcePropertyCompletion() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${res_a.b.<caret>}'", "count", "x");
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${concat(res_a.b.<caret>)}'", "count", "x");
  }

}
