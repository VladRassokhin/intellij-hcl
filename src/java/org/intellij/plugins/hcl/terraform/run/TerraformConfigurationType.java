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
package org.intellij.plugins.hcl.terraform.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.intellij.plugins.hcl.HCLBundle;
import org.intellij.plugins.hcl.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TerraformConfigurationType implements ConfigurationType {
  private final ConfigurationFactory myBaseFactory;
  private final ConfigurationFactory myPlanFactory;
  private final ConfigurationFactory myApplyFactory;

  public TerraformConfigurationType() {
    myBaseFactory = new MyConfigurationFactory(null, null);
    myPlanFactory = new MyConfigurationFactory("Plan", "plan");
    myApplyFactory = new MyConfigurationFactory("Apply", "apply");
  }

  @NotNull
  public static TerraformConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(TerraformConfigurationType.class);
  }

  public ConfigurationFactory getBaseFactory() {
    return myBaseFactory;
  }

  public ConfigurationFactory getPlanFactory() {
    return myPlanFactory;
  }

  public ConfigurationFactory getApplyFactory() {
    return myApplyFactory;
  }

  @Override
  public String getDisplayName() {
    return HCLBundle.message("terraform.configuration.title");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return HCLBundle.message("terraform.configuration.type.description");
  }

  @Override
  public Icon getIcon() {
    return Icons.FileTypes.INSTANCE.getTerraform();
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{myBaseFactory, myPlanFactory, myApplyFactory};
  }

  @NotNull
  @Override
  public String getId() {
    return "#org.intellij.plugins.hcl.terraform.run.TerraformConfigurationType";
  }

  private class MyConfigurationFactory extends ConfigurationFactory {
    private final String myParameters;
    private final String myNameSuffix;

    public MyConfigurationFactory(String nameSuffix, String parameters) {
      super(TerraformConfigurationType.this);
      myNameSuffix = nameSuffix;
      myParameters = parameters;
    }

    @Override
    public String getName() {
      final String name = super.getName();
      if (myNameSuffix != null) return name + " " + myNameSuffix;
      return name;
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      TerraformRunConfiguration configuration = new TerraformRunConfiguration(project, this, "");
      String path = project.getBasePath();
      if (path != null) {
        configuration.setWorkingDirectory(path);
      }
      if (myParameters != null) {
        configuration.setProgramParameters(myParameters);
      }
      return configuration;
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
      // TODO: Implement
      return true;
    }

    @Override
    public boolean isConfigurationSingletonByDefault() {
      return true;
    }
  }
}
