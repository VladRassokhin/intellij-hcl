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
package org.intellij.plugins.hcl.terraform.config;

import com.intellij.testFramework.InspectionFixtureTestCase;
import org.intellij.plugins.hcl.terraform.config.inspection.*;
import org.intellij.plugins.hil.inspection.HILMissingSelfInContextInspection;
import org.intellij.plugins.hil.inspection.HILOperationTypesMismatchInspection;
import org.intellij.plugins.hil.inspection.HILUnresolvedReferenceInspection;

public class TerraformInspectionsTestCase extends InspectionFixtureTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.setTestDataPath(getBasePath());
  }

  @Override
  protected String getBasePath() {
    return "test-data/terraform/inspections/";
  }

  public void testResourcePropertyReferences() throws Exception {
    doTest("resource_property_reference", new HILUnresolvedReferenceInspection());
  }

  public void testMappingVariableReference() throws Exception {
    doTest("mapping_variable_reference", new HILUnresolvedReferenceInspection());
  }

  public void testWeirdBlockComputedPropertyReference() throws Exception {
    doTest("weird_block_computed_property_reference", new HILUnresolvedReferenceInspection());
  }

  public void testKnownBlockNameFromModel() throws Exception {
    doTest("unknown_block_name", new HCLUnknownBlockTypeInspection());
  }

  // Test for issue #198
  public void testNoUnknownBlocksForNomad() throws Exception {
    doTest("no_unknown_blocks_for_nomad", new HCLUnknownBlockTypeInspection());
  }

  public void testIncorrectTFVARS() throws Exception {
    doTest("incorrect_tfvars", new TFVARSIncorrectElementInspection());
  }

  public void testIncorrectVariableType() throws Exception {
    doTest("incorrect_variable_type", new TFIncorrectVariableTypeInspection());
  }

  public void testDuplicatedProvider() throws Exception {
    doTest("duplicated_provider", new TFDuplicatedProviderInspection());
  }

  public void testDuplicatedOutput() throws Exception {
    doTest("duplicated_output", new TFDuplicatedOutputInspection());
  }

  public void testDuplicatedVariable() throws Exception {
    doTest("duplicated_variable", new TFDuplicatedVariableInspection());
  }

  public void testDuplicatedBlockProperty() throws Exception {
    doTest("duplicated_block_property", new TFDuplicatedBlockPropertyInspection());
  }

  public void testInterpolationsInWrongPlaces() throws Exception {
    doTest("interpolations_in_wrong_places", new TFNoInterpolationsAllowedInspection());
  }

  public void testMissingBlockProperty() throws Exception {
    doTest("missing_properties", new HCLBlockMissingPropertyInspection());
  }

  public void testConflictingBlockProperty() throws Exception {
    doTest("conflicting_properties", new HCLBlockConflictingPropertiesInspection());
  }

  public void testMissingSelfInContext() throws Exception {
    doTest("reference_to_self", new HILMissingSelfInContextInspection());
  }

  public void testInterpolationBinaryExpressionsTypesCheck() throws Exception {
    doTest("interpolation_operations_types", new HILOperationTypesMismatchInspection());
  }

}
