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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.testFramework.LightPlatformTestCase;
import org.intellij.plugins.hcl.terraform.config.model.*;

public class TerraformModelProviderTest extends LightPlatformTestCase {
  public void testModelIsLoaded() throws Exception {
    final TypeModelProvider provider = ServiceManager.getService(TypeModelProvider.class);
    final TypeModel model = provider.get();
  }

  public void testProperlyParsedNetworkInterface() throws Exception {
    final TypeModelProvider provider = ServiceManager.getService(TypeModelProvider.class);
    assertNotNull(provider);
    final TypeModel model = provider.get();
    assertNotNull(model);
    final ResourceType google_compute_instance = model.getResourceType("google_compute_instance");
    assertNotNull(google_compute_instance);
    final PropertyOrBlockType[] properties = google_compute_instance.getProperties();
    final PropertyOrBlockType network_interface = findProperty(properties, "network_interface");
    assertNotNull(network_interface);
    final BlockType network_interfaceBlock = network_interface.getBlock();
    assertNotNull(network_interfaceBlock);
    final PropertyOrBlockType access_config = findProperty(network_interfaceBlock.getProperties(), "access_config");
    assertNotNull(access_config);
    final BlockType access_configBlock = access_config.getBlock();
    assertNotNull(access_configBlock);
    assertNotNull(findProperty(access_configBlock.getProperties(), "assigned_nat_ip"));
    assertNotNull(findProperty(access_configBlock.getProperties(), "nat_ip"));
  }

  private PropertyOrBlockType findProperty(PropertyOrBlockType[] properties, String name) {
    for (PropertyOrBlockType property : properties) {
      if (name.equals(property.getName())) return property;
    }
    return null;
  }
}
