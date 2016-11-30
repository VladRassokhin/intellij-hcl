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
package org.intellij.plugins.hcl.terraform;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import org.intellij.plugins.hcl.navigation.HCLQualifiedNameProvider;
import org.intellij.plugins.hcl.psi.HCLBlock;
import org.intellij.plugins.hcl.psi.HCLElementGenerator;
import org.intellij.plugins.hcl.psi.HCLObject;
import org.intellij.plugins.hcl.terraform.config.psi.TerraformElementGenerator;

public class ModelFQNTest extends LightPlatformTestCase {
  protected HCLElementGenerator myElementGenerator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myElementGenerator = new TerraformElementGenerator(getProject());
  }

  public void testResourceDefinitionBlock() throws Exception {
    HCLBlock block = (HCLBlock) myElementGenerator.createDummyFile("resource 'A' 'B' {}").getFirstChild();
    checkFQN("resource.A", block.getNameElements()[2]);
    checkFQN("resource.A", block.getNameElements()[1]);
    checkFQN("resource.A", block.getNameElements()[0]);
    checkFQN("resource.A", block);
  }

  public void testDataSourceDefinitionBlock() throws Exception {
    HCLBlock block = (HCLBlock) myElementGenerator.createDummyFile("data 'A' 'B' {}").getFirstChild();
    checkFQN("data.A", block.getNameElements()[2]);
    checkFQN("data.A", block.getNameElements()[1]);
    checkFQN("data.A", block.getNameElements()[0]);
    checkFQN("data.A", block);
  }

  public void testProviderDefinitionBlock() throws Exception {
    HCLBlock block = (HCLBlock) myElementGenerator.createDummyFile("provider 'X' {}").getFirstChild();
    checkFQN("provider.X", block.getNameElements()[1]);
    checkFQN("provider.X", block.getNameElements()[0]);
    checkFQN("provider.X", block);
  }

  public void testDataSourceInnerBlock() throws Exception {
    PsiFile file = myElementGenerator.createDummyFile("data \"consul_keys\" \"demo\" {\n" +
        "  var {\n" +
        "  }\n" +
        "}\n");
    HCLBlock[] blocks = PsiTreeUtil.getChildrenOfType(file, HCLBlock.class);
    assertNotNull(blocks);
    HCLObject object = blocks[0].getObject();
    assertNotNull(object);
    HCLBlock vars = PsiTreeUtil.getChildOfType(object, HCLBlock.class);
    assertNotNull(vars);
    checkFQN("data.consul_keys.var", vars);
  }

  private void checkFQN(String expected, PsiElement element) {
    String fqn = HCLQualifiedNameProvider.Companion.getQualifiedModelName(element);
    assertEquals(expected, fqn);
  }
}
