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
package org.intellij.plugins.hcl.terraform.config.psi;

import org.intellij.plugins.hcl.psi.*;
import org.intellij.plugins.hcl.terraform.config.model.Types;
import org.jetbrains.annotations.NotNull;

public class TerraformElementGeneratorTest extends HCLElementGeneratorTest {
  @NotNull
  @Override
  protected HCLElementGenerator createElementGenerator() {
    return new TerraformElementGenerator(getProject());
  }

  @Override
  public void testCreateNumericalValue() throws Exception {
    super.testCreateNumericalValue();

    HCLValue element = myElementGenerator.createValue("1GB");
    assertTrue(element instanceof HCLNumberLiteral);
    Number value = ((HCLNumberLiteral) element).getValue();
    assertInstanceOf(value, Integer.class);
    assertEquals(1024 * 1024 * 1024, value);

    element = myElementGenerator.createValue("10GB");
    assertTrue(element instanceof HCLNumberLiteral);
    value = ((HCLNumberLiteral) element).getValue();
    assertInstanceOf(value, Long.class);
    assertEquals(10 * 1024L * 1024L * 1024L, value);
  }

  public void testCreateVariable() throws Exception {
    TerraformElementGenerator generator = (TerraformElementGenerator) myElementGenerator;
    HCLBlock element = generator.createVariable("name", Types.INSTANCE.getString(), "\"42\"");
    assertEquals("name", element.getName());
    HCLObject object = element.getObject();
    assertNotNull(object);
    HCLProperty property = object.findProperty("default");
    assertNotNull(property);
    HCLValue value = property.getValue();
    assertNotNull(value);
    assertTrue(value instanceof HCLStringLiteral);
    assertEquals("42", ((HCLStringLiteral) value).getValue());
  }
}
