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

    // Terraform 0.10.0
    doTestModuleStorage("b80c279f2a978db6d144a7d9fe67e3a4", "sub", "subdir");
    doTestModuleStorage("6a201418d8e5ec3adb85f716bfcd701b", "sub", "./subdir");

    doTestModuleStorage("7b5c0ad0db58e5dbc3e2bc5b16f1719d", "remote-1", "github.com/hashicorp/consul/terraform/aws");
    doTestModuleStorage("1818d346a2ff27c9d4612c699b67405f", "remote-2", "github.com/hashicorp/consul//terraform/aws");
    doTestModuleStorage("78878398d5f76669428cf3f337ad5327", "remote-3", "git@github.com:hashicorp/consul.git//terraform/aws");
  }

  public void testAdditionalPathDetection() throws Exception {
    // GitHub
    doTestModuleSourceAdditionalPath(null, "github.com/hashicorp/consul");
    doTestModuleSourceAdditionalPath("terraform/aws", "github.com/hashicorp/consul/terraform/aws");
    doTestModuleSourceAdditionalPath("terraform/aws", "github.com/hashicorp/consul//terraform/aws");
    doTestModuleSourceAdditionalPath("terraform/aws", "git@github.com:hashicorp/consul.git//terraform/aws");
    // GitHub private
    doTestModuleSourceAdditionalPath("modules/foo", "git::https://MACHINE-USER:MACHINE-PASS@github.com/org/privatemodules//modules/foo");

    // BitBucket
    doTestModuleSourceAdditionalPath(null, "bitbucket.org/hashicorp/consul");
//    doTestModuleSourceAdditionalPath("terraform/aws", "bitbucket.org/hashicorp/consul/terraform/aws");
    doTestModuleSourceAdditionalPath("terraform/aws", "bitbucket.org/hashicorp/consul//terraform/aws");
    // BitBucket private
    doTestModuleSourceAdditionalPath(null, "git::https://bitbucket.org/foocompany/module_name.git");
    doTestModuleSourceAdditionalPath(null, "git::https://bitbucket.org/foocompany/module_name.git?ref=hotfix");

    // Generic Git
    doTestModuleSourceAdditionalPath(null, "git://hashicorp.com/consul.git");
    doTestModuleSourceAdditionalPath(null, "git::https://hashicorp.com/consul.git");
    doTestModuleSourceAdditionalPath(null, "git::ssh://git@github.com/owner/repo.git");
    doTestModuleSourceAdditionalPath(null, "git::https://hashicorp.com/consul.git?ref=master");


    // Generic Mercurial
    doTestModuleSourceAdditionalPath(null, "hg::http://hashicorp.com/consul.hg");
    doTestModuleSourceAdditionalPath("terraform/aws", "hg::http://hashicorp.com/consul.hg//terraform/aws");
    doTestModuleSourceAdditionalPath(null, "hg::http://hashicorp.com/consul.hg?rev=default");
    doTestModuleSourceAdditionalPath("terraform/aws", "hg::http://hashicorp.com/consul.hg//terraform/aws?rev=default");

    // S3
    doTestModuleSourceAdditionalPath(null, "s3::https://s3-eu-west-1.amazonaws.com/consulbucket/consul.zip");
    doTestModuleSourceAdditionalPath("terraform/aws", "s3::https://s3-eu-west-1.amazonaws.com/consulbucket/consul.zip//terraform/aws");
    doTestModuleSourceAdditionalPath(null, "consulbucket.s3-eu-west-1.amazonaws.com/consul.zip");

    // 'file:'
    doTestModuleSourceAdditionalPath(null, "");
  }

  private static void doTestModuleStorage(String expected, String name, String source) {
    assertEquals(expected, ModuleDetectionUtil.INSTANCE.computeModuleStorageName(name, source));
  }

  private static void doTestModuleSourceAdditionalPath(String expected, String source) {
    assertEquals(expected, ModuleDetectionUtil.INSTANCE.getModuleSourceAdditionalPath(source));
  }
}
