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

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ExternalizablePath;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.plugins.hcl.HCLBundle;
import org.intellij.plugins.hcl.terraform.TerraformToolProjectSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class TerraformRunConfiguration extends RunConfigurationBase implements CommonProgramRunConfigurationParameters {
  public String PROGRAM_PARAMETERS;
  public String WORKING_DIRECTORY;
  private final Map<String, String> myEnvs = new LinkedHashMap<String, String>();
  public boolean PASS_PARENT_ENVS = true;

  public TerraformRunConfiguration(final Project project, final ConfigurationFactory factory, final String name) {
    super(project, factory, name);
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new TerraformRunConfigurationEditor();
  }

  @Override
  public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
    final String error = getError();
    if (error != null) {
      throw new ExecutionException(error);
    }

    return new CommandLineState(env) {
      @NotNull
      @Override
      protected ProcessHandler startProcess() throws ExecutionException {
        OSProcessHandler handler;
        handler = KillableColoredProcessHandler.create(createCommandLine());
        ProcessTerminatedListener.attach(handler);
        return handler;
      }

      protected GeneralCommandLine createCommandLine() throws ExecutionException {
        final GeneralCommandLine gcl = new GeneralCommandLine();
        final SimpleProgramParameters parameters = getParameters();

        gcl.setExePath(TerraformToolProjectSettings.getInstance(getProject()).getTerraformPath());
        gcl.withWorkDirectory(parameters.getWorkingDirectory());
        gcl.getParametersList().addAll(parameters.getProgramParametersList().getParameters());
        gcl.setPassParentEnvironment(parameters.isPassParentEnvs());
        gcl.withEnvironment(parameters.getEnv());

        return gcl;
      }

      protected SimpleProgramParameters getParameters() throws ExecutionException {
        final SimpleProgramParameters params = new SimpleProgramParameters();

        fillParameterList(params.getProgramParametersList(), PROGRAM_PARAMETERS);

        params.setWorkingDirectory(getWorkingDirectory());

        return params;
      }
    };
  }

  private static void fillParameterList(ParametersList list, @Nullable String value) {
    if (value == null) return;

    for (String parameter : value.split(" ")) {
      if (parameter != null && parameter.length() > 0) {
        list.add(parameter);
      }
    }
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (StringUtil.isEmptyOrSpaces(WORKING_DIRECTORY)) {
      RuntimeConfigurationException exception = new RuntimeConfigurationException(HCLBundle.message("run.configuration.no.working.directory.specified"));
      exception.setQuickFix(new Runnable() {
        @Override
        public void run() {
          setWorkingDirectory(getProject().getBasePath());
        }
      });
      throw exception;
    }

    final String error = getError();
    if (error != null) {
      throw new RuntimeConfigurationException(error);
    }
  }

  private String getError() {
    if (StringUtil.isEmptyOrSpaces(WORKING_DIRECTORY)) {
      return (HCLBundle.message("run.configuration.no.working.directory.specified"));
    }
    final String terraformPath = TerraformToolProjectSettings.getInstance(getProject()).getTerraformPath();
    if (StringUtil.isEmptyOrSpaces(terraformPath)) {
      return (HCLBundle.message("run.configuration.no.terraform.specified"));
    }
    if (!FileUtil.canExecute(new File(terraformPath))) {
      return (HCLBundle.message("run.configuration.terraform.path.incorrect"));
    }
    return null;
  }

  @Override
  public void setProgramParameters(String value) {
    PROGRAM_PARAMETERS = value;
  }

  @Override
  public String getProgramParameters() {
    return PROGRAM_PARAMETERS;
  }

  @Override
  public void setWorkingDirectory(String value) {
    WORKING_DIRECTORY = ExternalizablePath.urlValue(value);
  }

  @Override
  public String getWorkingDirectory() {
    return ExternalizablePath.localPathValue(WORKING_DIRECTORY);
  }

  @Override
  public void setPassParentEnvs(boolean passParentEnvs) {
    PASS_PARENT_ENVS = passParentEnvs;
  }

  @Override
  @NotNull
  public Map<String, String> getEnvs() {
    return myEnvs;
  }

  @Override
  public void setEnvs(@NotNull final Map<String, String> envs) {
    myEnvs.clear();
    myEnvs.putAll(envs);
  }

  @Override
  public boolean isPassParentEnvs() {
    return PASS_PARENT_ENVS;
  }

  @Override
  public void readExternal(final Element element) throws InvalidDataException {
    super.readExternal(element);
    //noinspection deprecation
    DefaultJDOMExternalizer.readExternal(this, element);
    EnvironmentVariablesComponent.readExternal(element, getEnvs());
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    //noinspection deprecation
    DefaultJDOMExternalizer.writeExternal(this, element);
    EnvironmentVariablesComponent.writeExternal(element, getEnvs());
  }
}
