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
package org.intellij.plugins.hcl.terraform;

import com.intellij.lang.Language;
import com.intellij.openapi.components.ServiceManager;
import org.intellij.plugins.hcl.CompletionTestCase;
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage;
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType;
import org.intellij.plugins.hcl.terraform.config.model.ResourceType;
import org.intellij.plugins.hcl.terraform.config.model.TypeModelProvider;

import java.util.*;

public class TerraformConfigCompletionTest extends CompletionTestCase {

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
    return TerraformLanguage.INSTANCE$;
  }

  public void testBlockKeywordCompletion() throws Exception {
    doBasicCompletionTest("<caret> {}", TerraformConfigCompletionProvider.BLOCK_KEYWORDS);
    doBasicCompletionTest("a=1\n<caret> {}", TerraformConfigCompletionProvider.BLOCK_KEYWORDS);

    doBasicCompletionTest("<caret> ", TerraformConfigCompletionProvider.BLOCK_KEYWORDS);
    doBasicCompletionTest("a=1\n<caret> ", TerraformConfigCompletionProvider.BLOCK_KEYWORDS);
  }

  public void testNoBlockKeywordCompletion() throws Exception {
    doBasicCompletionTest("a={\n<caret>\n}", 0);
  }

  public void testResourceTypeCompletion() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    final TypeModelProvider provider = ServiceManager.getService(TypeModelProvider.class);
    for (ResourceType resource : provider.get().getResources()) {
      set.add(resource.getType());
    }
    doBasicCompletionTest("resource <caret>", set);
    doBasicCompletionTest("resource <caret> {}", set);
    doBasicCompletionTest("resource <caret> \"aaa\" {}", set);
  }

  public void testResourceQuotedTypeCompletion() throws Exception {
    final TreeSet<String> set = new TreeSet<String>();
    final TypeModelProvider provider = ServiceManager.getService(TypeModelProvider.class);
    for (ResourceType resource : provider.get().getResources()) {
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
    doBasicCompletionTest("resource abc {\n<caret>\n}", TerraformConfigCompletionProvider.COMMON_RESOURCE_PROPERTIES);
    final HashSet<String> set = new HashSet<String>(TerraformConfigCompletionProvider.COMMON_RESOURCE_PROPERTIES);
    set.remove("id");
    doBasicCompletionTest("resource \"x\" {\nid='a'\n<caret>\n}", set);
    doBasicCompletionTest("resource abc {\n<caret> = true\n}", Collections.<String>emptySet());
    doBasicCompletionTest("resource abc {\n<caret> {}\n}", Collections.singletonList("lifecycle"));
  }

  public void testResourceCommonPropertyCompletionFromModel() throws Exception {
    final HashSet<String> base = new HashSet<String>(TerraformConfigCompletionProvider.COMMON_RESOURCE_PROPERTIES);
    final TypeModelProvider provider = ServiceManager.getService(TypeModelProvider.class);
    final ResourceType type = provider.get().getResourceType("aws_instance");
    assertNotNull(type);
    for (PropertyOrBlockType it : type.getProperties()) {
      base.add(it.getName());
    }
    doBasicCompletionTest("resource aws_instance x {\n<caret>\n}", base);
    doBasicCompletionTest("resource aws_instance x {\n<caret> = \"name\"\n}", getPartialMatcher("provider", "ami"));
    doBasicCompletionTest("resource aws_instance x {\n<caret> = true\n}", 0);
    doBasicCompletionTest("resource aws_instance x {\n<caret> {}\n}", 0);
  }

}
