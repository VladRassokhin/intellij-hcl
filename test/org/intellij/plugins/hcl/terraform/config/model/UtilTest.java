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
package org.intellij.plugins.hcl.terraform.config.model;

import com.intellij.testFramework.UsefulTestCase;

public class UtilTest extends UsefulTestCase {
  public void testModuleStorageLocationCalculatedCorrectly() throws Exception {
    // From Terraform 0.8.1
    doTestModuleStorage("60db81a16b05caf2dfbc6adb0e78f370", "Z", "./mod");
    doTestModuleStorage("8aafe064612fb097dd5a647a7f4e6cb7", "bastion", "github.com/terraform-community-modules/tf_aws_bastion_s3_keys");
  }

  private static void doTestModuleStorage(String expected, String name, String source) {
    assertEquals(expected, UtilKt.computeModuleStorageName(name, source));
  }
}
