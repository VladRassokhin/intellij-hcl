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
package org.intellij.plugins.hcl.terraform;

import com.intellij.testFramework.LightPlatformTestCase;
import org.intellij.plugins.hcl.terraform.config.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

public class TerraformModelProviderTest extends LightPlatformTestCase {
  public void testModelIsLoaded() throws Exception {
    //noinspection unused
    final TypeModel model = TypeModelProvider.Companion.getModel(getProject());
  }

  public void testProperlyParsedNetworkInterface() throws Exception {
    final TypeModel model = TypeModelProvider.Companion.getModel(getProject());
    assertNotNull(model);
    final ResourceType google_compute_instance = model.getResourceType("google_compute_instance");
    assertNotNull(google_compute_instance);
    final PropertyOrBlockType[] properties = google_compute_instance.getProperties();
    final PropertyOrBlockType network_interface = findProperty(properties, "network_interface");
    assertNotNull(network_interface);
    final BlockType network_interfaceBlock = (BlockType) network_interface;
    assertNotNull(network_interfaceBlock);
    final PropertyOrBlockType access_config = findProperty(network_interfaceBlock.getProperties(), "access_config");
    assertNotNull(access_config);
    final BlockType access_configBlock = (BlockType) access_config;
    assertNotNull(access_configBlock);
    assertNotNull(findProperty(access_configBlock.getProperties(), "assigned_nat_ip"));
    assertNotNull(findProperty(access_configBlock.getProperties(), "nat_ip"));
  }

  // Test for #67
  public void test_aws_cloudfront_distribution_forwarded_values() throws Exception {
    final TypeModel model = TypeModelProvider.Companion.getModel(getProject());
    assertNotNull(model);

    final ResourceType aws_cloudfront_distribution = model.getResourceType("aws_cloudfront_distribution");
    assertNotNull(aws_cloudfront_distribution);
    final PropertyOrBlockType[] properties = aws_cloudfront_distribution.getProperties();

    final PropertyOrBlockType default_cache_behavior = findProperty(properties, "default_cache_behavior");
    assertNotNull(default_cache_behavior);
    final BlockType default_cache_behavior_block = (BlockType) default_cache_behavior;
    assertNotNull(default_cache_behavior_block);

    final PropertyOrBlockType forwarded_values = findProperty(default_cache_behavior_block.getProperties(), "forwarded_values");
    assertNotNull(forwarded_values);
    final BlockType forwarded_values_block = (BlockType) forwarded_values;
    assertNotNull(forwarded_values_block);

    assertNotNull(findProperty(forwarded_values_block.getProperties(), "query_string"));

    PropertyOrBlockType cookies = findProperty(forwarded_values_block.getProperties(), "cookies");
    assertNotNull(cookies);
    assertTrue(cookies.getRequired());
  }

  public void testAllResourceHasProviderNameAsPrefix() throws Exception {
    final TypeModel model = TypeModelProvider.Companion.getModel(getProject());
    assertNotNull(model);
    final List<ResourceType> failedResources = new ArrayList<>();
    for (ResourceType block : model.getResources().values()) {
      final String rt = block.getType();
      String pt = block.getProvider().getType();
      if (pt.equals("azure-classic")) {
        pt = "azure";
      }
      if (rt.equals(pt)) continue;
      if (rt.startsWith(pt + '_')) continue;
      failedResources.add(block);
    }
    then(failedResources).isEmpty();
  }

  public void testDataSourcesHasProviderNameAsPrefix() throws Exception {
    final TypeModel model = TypeModelProvider.Companion.getModel(getProject());
    assertNotNull(model);
    final List<DataSourceType> failedDataSources = new ArrayList<>();
    for (DataSourceType block : model.getDataSources().values()) {
      final String rt = block.getType();
      String pt = block.getProvider().getType();
      if (pt.equals("azure-classic")) {
        pt = "azure";
      }
      if (rt.equals(pt)) continue;
      if (rt.startsWith(pt + '_')) continue;
      failedDataSources.add(block);
    }
    then(failedDataSources).isEmpty();
  }

  private PropertyOrBlockType findProperty(PropertyOrBlockType[] properties, String name) {
    for (PropertyOrBlockType property : properties) {
      if (name.equals(property.getName())) return property;
    }
    return null;
  }
}
