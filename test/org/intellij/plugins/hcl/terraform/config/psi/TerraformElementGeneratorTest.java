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
package org.intellij.plugins.hcl.terraform.config.psi;

import org.intellij.plugins.hcl.psi.HCLElementGenerator;
import org.intellij.plugins.hcl.psi.HCLElementGeneratorTest;
import org.intellij.plugins.hcl.psi.HCLNumberLiteral;
import org.intellij.plugins.hcl.psi.HCLValue;
import org.jetbrains.annotations.NotNull;

public class TerraformElementGeneratorTest extends HCLElementGeneratorTest {
  @NotNull
  @Override
  protected HCLElementGenerator createElementGenerator() {
    return new TerraformElementGenerator(getProject());
  }

  @Override
  public void testCreateNumericalValue() throws Exception {
    HCLValue element = myElementGenerator.createValue("42");
    assertTrue(element instanceof HCLNumberLiteral);
    assertEquals(42.0, ((HCLNumberLiteral) element).getValue());
    element = myElementGenerator.createValue("10KB");
    assertTrue(element instanceof HCLNumberLiteral);
    assertEquals(10 * 1024.0 * 1024.0, ((HCLNumberLiteral) element).getValue());
  }
}
