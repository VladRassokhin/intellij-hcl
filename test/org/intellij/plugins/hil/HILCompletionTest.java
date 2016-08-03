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
package org.intellij.plugins.hil;

import com.intellij.lang.Language;
import com.intellij.util.BooleanFunction;
import org.intellij.plugins.hcl.CompletionTestCase;
import org.intellij.plugins.hil.codeinsight.HILCompletionContributor;

import java.util.Collection;

import static org.assertj.core.api.BDDAssertions.then;

public class HILCompletionTest extends CompletionTestCase {
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
    return HILLanguage.INSTANCE;
  }

  public void testMethodCompletion_BeginOnInterpolation() throws Exception {
    doBasicCompletionTest("a='${<caret>}'", getPartialMatcher(HILCompletionContributor.GLOBAL_AVAILABLE));
  }

  public void testMethodCompletion_AsParameter() throws Exception {
    doBasicCompletionTest("a='${foo(<caret>)}'", getPartialMatcher(HILCompletionContributor.GLOBAL_AVAILABLE));
    doBasicCompletionTest("a='${foo(true,<caret>)}'", getPartialMatcher(HILCompletionContributor.GLOBAL_AVAILABLE));
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
    doBasicCompletionTest("data 'y' 'x' {count = 2 source='${count.<caret>}'", 1, "index");
    doBasicCompletionTest("data 'y' 'x' {source='${count.<caret>}'", 1, "index");
    doBasicCompletionTest("data 'y' 'x' {count = 2 source='${count.<caret> + 1}'", 1, "index");
  }

  public void testModuleCompletion() throws Exception {
    doBasicCompletionTest("module 'ref' {source = './child'} foo='${module.<caret>}'", 1, "ref");
  }

  //<editor-fold desc="Resource completion">
  public void testResourceTypeCompletion() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${<caret>}'", "res_a");
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${concat(<caret>)}'", "res_a");
  }

  public void testResourceNameCompletion() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${res_a.<caret>}'", "b");
    doBasicCompletionTest("resource 'res_a' 'b' {} foo='${concat(res_a.<caret>)}'", "b");
    doBasicCompletionTest("resource 'res_a' 'b' {}\ndata 'data_a' 'c' {}\n foo='${res_a.<caret>}'", new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).contains("b").doesNotContain("c");
        return true;
      }
    });
  }

  public void testResourcePropertyCompletion() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${res_a.b.<caret>}'", "count", "x");
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${concat(res_a.b.<caret>)}'", "count", "x");
  }

  public void testResourcePropertyCompletionAfterNumber() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${res_a.b.1.<caret>}'", "count", "x");
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${concat(res_a.b.1.<caret>)}'", "count", "x");
  }

  public void testResourcePropertyCompletionAfterStar() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${res_a.b.*.<caret>}'", "count", "x");
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${concat(res_a.b.*.<caret>)}'", "count", "x");
  }

  public void testResourcePropertyCompletionAfterQuotedStar() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${res_a.b.\"*\".<caret>}'", "count", "x");
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${concat(res_a.b.\"*\".<caret>)}'", "count", "x");
  }

  public void testResourcePropertyCompletionQuotedResourceName() throws Exception {
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${res_a.\"b\".<caret>}'", 0);
    doBasicCompletionTest("resource 'res_a' 'b' {x='y'} foo='${concat(res_a.\"b\".<caret>)}'", 0);
  }
  //</editor-fold>


  //<editor-fold desc="Data source completion">
  public void testDataSourceTypeCompletion() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {} foo='${data.<caret>}'", "data_a");
    doBasicCompletionTest("data 'data_a' 'b' {} foo='${concat(data.<caret>)}'", "data_a");
  }

  public void testDataSourceNameCompletion() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {} foo='${data.data_a.<caret>}'", 1, "b");
    doBasicCompletionTest("data 'data_a' 'b' {} foo='${concat(data.data_a.<caret>)}'", 1, "b");

    doBasicCompletionTest("data 'data_a' 'b' {}\nresource 'res_a' 'a' {}\nfoo='${data.data_a.<caret>}'", 1, "b");
  }

  public void testDataSourcePropertyCompletion() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${data.data_a.b.<caret>}'", "count", "x");
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${concat(data.data_a.b.<caret>)}'", "count", "x");
  }

  public void testDataSourcePropertyCompletionAfterNumber() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${data.data_a.b.1.<caret>}'", "count", "x");
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${concat(data.data_a.b.1.<caret>)}'", "count", "x");
  }

  public void testDataSourcePropertyCompletionAfterSelect() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${data.data_a.b[1].<caret>}'", "count", "x");
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${concat(data.data_a.b[1].<caret>)}'", "count", "x");
  }

  public void testDataSourcePropertyCompletionAfterStar() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${data.data_a.b.*.<caret>}'", "count", "x");
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${concat(data.data_a.b.*.<caret>)}'", "count", "x");
  }

  public void testDataSourcePropertyCompletionAfterQuotedStar() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${data.data_a.b.\"*\".<caret>}'", "count", "x");
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${concat(data.data_a.b.\"*\".<caret>)}'", "count", "x");
  }

  public void testDataSourcePropertyCompletionQuotedDataSourceName() throws Exception {
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${data.data_a.\"b\".<caret>}'", 0);
    doBasicCompletionTest("data 'data_a' 'b' {x='y'} foo='${concat(data.data_a.\"b\".<caret>)}'", 0);
  }
  //</editor-fold>

}
