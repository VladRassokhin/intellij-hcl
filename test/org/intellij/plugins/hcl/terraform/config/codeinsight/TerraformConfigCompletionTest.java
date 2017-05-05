/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.config.codeinsight;

import com.intellij.util.BooleanFunction;
import org.intellij.plugins.hcl.terraform.config.model.*;

import java.util.*;

import static org.assertj.core.api.BDDAssertions.then;

public class TerraformConfigCompletionTest extends TFBaseCompletionTestCase {

  public void testBlockKeywordCompletion() throws Exception {
    doBasicCompletionTest("<caret> {}", TerraformConfigCompletionContributor.ROOT_BLOCK_KEYWORDS);
    doBasicCompletionTest("a=1\n<caret> {}", TerraformConfigCompletionContributor.ROOT_BLOCK_KEYWORDS);

    doBasicCompletionTest("<caret> ", TerraformConfigCompletionContributor.ROOT_BLOCK_KEYWORDS);
    doBasicCompletionTest("a=1\n<caret> ", TerraformConfigCompletionContributor.ROOT_BLOCK_KEYWORDS);
  }

  public void testNoBlockKeywordCompletion() throws Exception {
    doBasicCompletionTest("a={\n<caret>\n}", 0);
  }

  //<editor-fold desc="Resources completion tests">
  public void testResourceTypeCompletion() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    for (ResourceType resource : TypeModelProvider.Companion.getModel(getProject()).getResources()) {
      set.add(resource.getType());
    }
    doBasicCompletionTest("resource <caret>", set);
    doBasicCompletionTest("resource <caret> {}", set);
    doBasicCompletionTest("resource <caret> \"aaa\" {}", set);
  }

  public void testResourceQuotedTypeCompletion() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    for (ResourceType resource : TypeModelProvider.Companion.getModel(getProject()).getResources()) {
      set.add(resource.getType());
    }
    doBasicCompletionTest("resource \"<caret>", set);
    doBasicCompletionTest("resource \'<caret>", set);
    doBasicCompletionTest("resource \"<caret>\n{}", set);
    doBasicCompletionTest("resource \'<caret>\n{}", set);
    doBasicCompletionTest("resource \"<caret>\" {}", set);
    doBasicCompletionTest("resource \"<caret>\" \"aaa\" {}", set);
  }

  public void testResourceCommonPropertyCompletion() throws Exception {
    doBasicCompletionTest("resource abc {\n<caret>\n}", COMMON_RESOURCE_PROPERTIES);
    final HashSet<String> set = new HashSet<String>(COMMON_RESOURCE_PROPERTIES);
    set.remove("id");
    doBasicCompletionTest("resource \"x\" {\nid='a'\n<caret>\n}", set);
    doBasicCompletionTest("resource abc {\n<caret> = true\n}", Collections.<String>emptySet());
    doBasicCompletionTest("resource abc {\n<caret> {}\n}", Arrays.asList("lifecycle", "connection", "provisioner"));
  }

  public void testResourceCommonPropertyCompletionFromModel() throws Exception {
    final HashSet<String> base = new HashSet<String>(COMMON_RESOURCE_PROPERTIES);
    final ResourceType type = TypeModelProvider.Companion.getModel(getProject()).getResourceType("aws_instance");
    assertNotNull(type);
    for (PropertyOrBlockType it : type.getProperties()) {
      base.add(it.getName());
    }
    doBasicCompletionTest("resource aws_instance x {\n<caret>\n}", base);
    doBasicCompletionTest("resource aws_instance x {\n<caret> = \"name\"\n}", "provider", "ami");
    doBasicCompletionTest("resource aws_instance x {\n<caret> = true\n}", "ebs_optimized", "monitoring");
    doBasicCompletionTest("resource aws_instance x {\n<caret> {}\n}", "lifecycle");

    // Should understand interpolation result
    doBasicCompletionTest("resource aws_instance x {\n<caret> = \"${true}\"\n}", new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).contains("ebs_optimized", "monitoring").doesNotContain("lifecycle", "provider", "ami");
        return true;
      }
    });
    // Or not
    doBasicCompletionTest("resource aws_instance x {\n<caret> = \"${}\"\n}", new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).contains("ebs_optimized", "monitoring", "provider", "ami").doesNotContain("lifecycle");
        return true;
      }
    });
  }

  public void testResourceProviderCompletionFromModel() throws Exception {
    doBasicCompletionTest("provider Z {}\nresource a b {provider=<caret>}", "Z");
    doBasicCompletionTest("provider Z {}\nresource a b {provider='<caret>'}", "Z");
    doBasicCompletionTest("provider Z {}\nresource a b {provider=\"<caret>\"}", "Z");
    doBasicCompletionTest("provider Z {alias='Y'}\nresource a b {provider=<caret>}", "Z.Y");
    doBasicCompletionTest("provider Z {alias='Y'}\nresource a b {provider='<caret>'}", "Z.Y");
    doBasicCompletionTest("provider Z {alias='Y'}\nresource a b {provider=\"<caret>\"}", "Z.Y");
  }

  public void testResourcePropertyCompletionBeforeInnerBlock() throws Exception {
    doBasicCompletionTest("resource abc {\n<caret>\nlifecycle {}\n}", COMMON_RESOURCE_PROPERTIES);
    final HashSet<String> set = new HashSet<String>(COMMON_RESOURCE_PROPERTIES);
    set.remove("id");
    doBasicCompletionTest("resource \"x\" {\nid='a'\n<caret>\nlifecycle {}\n}", set);
    doBasicCompletionTest("resource abc {\n<caret> = true\nlifecycle {}\n}", Collections.<String>emptySet());
  }

  public void testResourceDependsOnCompletion() throws Exception {
    doBasicCompletionTest("resource x y {}\nresource a b {depends_on=['<caret>']}", 1, "x.y");
    doBasicCompletionTest("resource x y {}\nresource a b {depends_on=[\"<caret>\"]}", 1, "x.y");
    doBasicCompletionTest("data x y {}\nresource a b {depends_on=['<caret>']}", 1, "data.x.y");
    doBasicCompletionTest("data x y {}\nresource a b {depends_on=[\"<caret>\"]}", 1, "data.x.y");

    // Only stings allowed in arrays, prevent other elements
    doBasicCompletionTest("resource x y {}\nresource a b {depends_on=[<caret>]}", 0);
    doBasicCompletionTest("data x y {}\nresource a b {depends_on=[<caret>]}", 0);
  }

  public void testResourceTypeCompletionGivenDefinedProvidersOrForNoPropsProviders() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    for (ResourceType resource : TypeModelProvider.Companion.getModel(getProject()).getResources()) {
      ProviderType provider = resource.getProvider();
      if (!provider.getType().equals("aws") && provider.getProperties().length != 0) continue;
      set.add(resource.getType());
    }
    then(set).contains("template_file", "aws_vpc");
    doBasicCompletionTest("provider aws {}\nresource <caret>", set);
    doBasicCompletionTest("provider aws {}\nresource <caret> {}", set);
    doBasicCompletionTest("provider aws {}\nresource <caret> \"aaa\" {}", set);
  }

  //</editor-fold>

  //<editor-fold desc="Data Sources completion tests">
  public void testDataSourceTypeCompletion() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    for (DataSourceType ds : TypeModelProvider.Companion.getModel(getProject()).getDataSources()) {
      set.add(ds.getType());
    }
    doBasicCompletionTest("data <caret>", set);
    doBasicCompletionTest("data <caret> {}", set);
    doBasicCompletionTest("data <caret> \"aaa\" {}", set);
  }

  public void testDataSourceQuotedTypeCompletion() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    for (DataSourceType ds : TypeModelProvider.Companion.getModel(getProject()).getDataSources()) {
      set.add(ds.getType());
    }
    doBasicCompletionTest("data \"<caret>", set);
    doBasicCompletionTest("data \'<caret>", set);
    doBasicCompletionTest("data \"<caret>\n{}", set);
    doBasicCompletionTest("data \'<caret>\n{}", set);
    doBasicCompletionTest("data \"<caret>\" {}", set);
    doBasicCompletionTest("data \"<caret>\" \"aaa\" {}", set);
  }

  public void testDataSourceCommonPropertyCompletion() throws Exception {
    doBasicCompletionTest("data abc {\n<caret>\n}", COMMON_DATA_SOURCE_PROPERTIES);
    final HashSet<String> set = new HashSet<String>(COMMON_DATA_SOURCE_PROPERTIES);
    set.remove("id");
    doBasicCompletionTest("data \"x\" {\nid='a'\n<caret>\n}", set);
    doBasicCompletionTest("data abc {\n<caret> = true\n}", Collections.<String>emptySet());
    doBasicCompletionTest("data abc {\n<caret> {}\n}", 0);
  }

  public void testDataSourceCommonPropertyCompletionFromModel() throws Exception {
    final HashSet<String> base = new HashSet<String>(COMMON_DATA_SOURCE_PROPERTIES);
    final DataSourceType type = TypeModelProvider.Companion.getModel(getProject()).getDataSourceType("aws_ecs_container_definition");
    assertNotNull(type);
    for (PropertyOrBlockType it : type.getProperties()) {
      base.add(it.getName());
    }
    doBasicCompletionTest("data aws_ecs_container_definition x {\n<caret>\n}", base);
    doBasicCompletionTest("data aws_ecs_container_definition x {\n<caret> = \"name\"\n}",
        "container_name",
        "task_definition",
        "image",
        "image_digest",
        "provider"
    );
    doBasicCompletionTest("data aws_ecs_container_definition x {\n<caret> = true\n}", "disable_networking");
    doBasicCompletionTest("data aws_ecs_container_definition x {\n<caret> {}\n}", "docker_labels", "environment");

    // Should understand interpolation result
    doBasicCompletionTest("data aws_ecs_container_definition x {\n<caret> = \"${true}\"\n}", new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).contains("disable_networking").doesNotContain(
            // Blocks
            "docker_labels", "environment",
            // Strings
            "container_name", "task_definition", "image", "image_digest", "provider");
        return true;
      }
    });
  }

  public void testDataSourceProviderCompletionFromModel() throws Exception {
    doBasicCompletionTest("provider Z {}\ndata a b {provider=<caret>}", "Z");
    doBasicCompletionTest("provider Z {}\ndata a b {provider='<caret>'}", "Z");
    doBasicCompletionTest("provider Z {}\ndata a b {provider=\"<caret>\"}", "Z");
    doBasicCompletionTest("provider Z {alias='Y'}\ndata a b {provider=<caret>}", "Z.Y");
    doBasicCompletionTest("provider Z {alias='Y'}\ndata a b {provider='<caret>'}", "Z.Y");
    doBasicCompletionTest("provider Z {alias='Y'}\ndata a b {provider=\"<caret>\"}", "Z.Y");
  }

  public void testDataSourceDependsOnCompletion() throws Exception {
    doBasicCompletionTest("resource x y {}\ndata a b {depends_on=['<caret>']}", 1, "x.y");
    doBasicCompletionTest("resource x y {}\ndata a b {depends_on=[\"<caret>\"]}", 1, "x.y");
    doBasicCompletionTest("data x y {}\ndata a b {depends_on=['<caret>']}", 1, "data.x.y");
    doBasicCompletionTest("data x y {}\ndata a b {depends_on=[\"<caret>\"]}", 1, "data.x.y");

    // Only stings allowed in arrays, prevent other elements
    doBasicCompletionTest("resource x y {}\ndata a b {depends_on=[<caret>]}", 0);
    doBasicCompletionTest("data x y {}\ndata a b {depends_on=[<caret>]}", 0);
  }

  public void testDataSourceTypeCompletionGivenDefinedProviders() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    for (DataSourceType ds : TypeModelProvider.Companion.getModel(getProject()).getDataSources()) {
      ProviderType provider = ds.getProvider();
      if (!provider.getType().equals("aws") && provider.getProperties().length != 0) continue;
      set.add(ds.getType());
    }
    then(set).contains("template_file", "aws_vpc");
    doBasicCompletionTest("provider aws {}\ndata <caret>", set);
    doBasicCompletionTest("provider aws {}\ndata <caret> {}", set);
    doBasicCompletionTest("provider aws {}\ndata <caret> \"aaa\" {}", set);
  }
  //</editor-fold>


  public void testVariableTypeCompletion() throws Exception {
    myCompleteInvocationCount = 1; // Ensure there would not be 'null', 'true' and 'false' variants
    doBasicCompletionTest("variable v { type = \"<caret>\" }", 3, "string", "map", "list");
    doBasicCompletionTest("variable v { type = <caret> }", 3, "string", "map", "list");
  }

  public void testSpecial_HasDynamicAttributes_Property_Not_Advised() throws Exception {
    doBasicCompletionTest("resource \"terraform_remote_state\" \"x\" { <caret> }", new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        then(strings).contains("backend").doesNotContain("__has_dynamic_attributes");
        return true;
      }
    });
  }
}
