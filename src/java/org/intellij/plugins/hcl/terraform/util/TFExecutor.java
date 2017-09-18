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
package org.intellij.plugins.hcl.terraform.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.ExecutionModes;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.*;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.plugins.hcl.terraform.TerraformConstants;
import org.intellij.plugins.hcl.terraform.TerraformToolProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class TFExecutor {
  private static final Logger LOGGER = Logger.getInstance(TFExecutor.class);
  @NotNull
  private final Map<String, String> myExtraEnvironment = ContainerUtil.newHashMap();
  @NotNull
  private final ParametersList myParameterList = new ParametersList();
  @NotNull
  private final ProcessOutput myProcessOutput = new ProcessOutput();
  @NotNull
  private final Project myProject;
  @Nullable
  private String myWorkDirectory;
  private boolean myShowOutputOnError;
  private boolean myShowNotificationsOnError;
  private boolean myShowNotificationsOnSuccess;
  private GeneralCommandLine.ParentEnvironmentType myParentEnvironmentType = GeneralCommandLine.ParentEnvironmentType.CONSOLE;
  private boolean myPtyDisabled;
  @Nullable
  private String myExePath;
  @Nullable
  private String myPresentableName;
  private OSProcessHandler myProcessHandler;
  private final Collection<ProcessListener> myProcessListeners = ContainerUtil.newArrayList();

  private TFExecutor(@NotNull Project project) {
    myProject = project;
    myExePath = TerraformToolProjectSettings.getInstance(myProject).getTerraformPath();
  }

  public static TFExecutor in(@NotNull Project project, @Nullable Module module) {
    return module != null ? in(module) : in(project);
  }

  @NotNull
  private static TFExecutor in(@NotNull Project project) {
    return new TFExecutor(project);
  }

  @NotNull
  public static TFExecutor in(@NotNull Module module) {
    // TODO: Decide whether 'module' is really needed
    return new TFExecutor(module.getProject());
  }

  @NotNull
  public TFExecutor withPresentableName(@Nullable String presentableName) {
    myPresentableName = presentableName;
    return this;
  }

  @NotNull
  public TFExecutor withExePath(@Nullable String exePath) {
    myExePath = exePath;
    return this;
  }

  @NotNull
  public TFExecutor withWorkDirectory(@Nullable String workDirectory) {
    myWorkDirectory = workDirectory;
    return this;
  }

  public TFExecutor withProcessListener(@NotNull ProcessListener listener) {
    myProcessListeners.add(listener);
    return this;
  }

  @NotNull
  public TFExecutor withExtraEnvironment(@NotNull Map<String, String> environment) {
    myExtraEnvironment.putAll(environment);
    return this;
  }

  @NotNull
  public TFExecutor withPassParentEnvironment(boolean passParentEnvironment) {
    myParentEnvironmentType = passParentEnvironment ? GeneralCommandLine.ParentEnvironmentType.CONSOLE
        : GeneralCommandLine.ParentEnvironmentType.NONE;
    return this;
  }

  @NotNull
  public TFExecutor withParameterString(@NotNull String parameterString) {
    myParameterList.addParametersString(parameterString);
    return this;
  }

  @NotNull
  public TFExecutor withParameters(@NotNull String... parameters) {
    myParameterList.addAll(parameters);
    return this;
  }

  @NotNull
  public TFExecutor withParameters(@NotNull List<String> parameters) {
    myParameterList.addAll(parameters);
    return this;
  }

  @NotNull
  public TFExecutor showOutputOnError() {
    myShowOutputOnError = true;
    return this;
  }

  @NotNull
  public TFExecutor disablePty() {
    myPtyDisabled = true;
    return this;
  }

  @NotNull
  public TFExecutor showNotifications(boolean onError, boolean onSuccess) {
    myShowNotificationsOnError = onError;
    myShowNotificationsOnSuccess = onSuccess;
    return this;
  }

  public boolean execute() {
    Logger.getInstance(getClass()).assertTrue(!ApplicationManager.getApplication().isDispatchThread(),
        "It's bad idea to run external tool on EDT");
    Logger.getInstance(getClass()).assertTrue(myProcessHandler == null, "Process has already run with this executor instance");
    final Ref<Boolean> result = Ref.create(false);
    GeneralCommandLine commandLine = null;
    try {
      commandLine = createCommandLine();
      GeneralCommandLine finalCommandLine = commandLine;
      myProcessHandler = new KillableColoredProcessHandler(finalCommandLine);
      final HistoryProcessListener historyProcessListener = new HistoryProcessListener();
      myProcessHandler.addProcessListener(historyProcessListener);
      for (ProcessListener listener : myProcessListeners) {
        myProcessHandler.addProcessListener(listener);
      }

      CapturingProcessAdapter processAdapter = new CapturingProcessAdapter(myProcessOutput) {
        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          super.processTerminated(event);
          boolean success = event.getExitCode() == 0 && myProcessOutput.getStderr().isEmpty();
          boolean nothingToShow = myProcessOutput.getStdout().isEmpty() && myProcessOutput.getStderr().isEmpty();
          boolean cancelledByUser = (event.getExitCode() == -1 || event.getExitCode() == 2) && nothingToShow;
          result.set(success);
          if (success) {
            if (myShowNotificationsOnSuccess) {
              showNotification("Finished successfully", NotificationType.INFORMATION);
            }
          } else if (cancelledByUser) {
            if (myShowNotificationsOnError) {
              showNotification("Interrupted", NotificationType.WARNING);
            }
          } else if (myShowOutputOnError) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                showOutput(myProcessHandler, historyProcessListener);
              }
            });
          }
        }
      };

      myProcessHandler.addProcessListener(processAdapter);
      myProcessHandler.startNotify();
      ExecutionModes.SameThreadMode sameThreadMode = new ExecutionModes.SameThreadMode(getPresentableName());
      ExecutionHelper.executeExternalProcess(myProject, myProcessHandler, sameThreadMode, commandLine);

      LOGGER.debug("Finished `" + getPresentableName() + "` with result: " + result.get());
      return result.get();
    } catch (ExecutionException e) {
      if (myShowOutputOnError) {
        ExecutionHelper.showErrors(myProject, Collections.singletonList(e), getPresentableName(), null);
      }
      if (myShowNotificationsOnError) {
        showNotification(StringUtil.notNullize(e.getMessage(), "Unknown error, see logs for details"), NotificationType.ERROR);
      }
      String commandLineInfo = commandLine != null ? commandLine.getCommandLineString() : "not constructed";
      LOGGER.debug("Finished `" + getPresentableName() + "` with an exception. Commandline: " + commandLineInfo, e);
      return false;
    }
  }

  public void executeWithProgress(boolean modal) {
    //noinspection unchecked
    executeWithProgress(modal, Consumer.EMPTY_CONSUMER);
  }

  public void executeWithProgress(final boolean modal, @NotNull final Consumer<Boolean> consumer) {
    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, getPresentableName(), true) {
      private boolean doNotStart;

      @Override
      public void onCancel() {
        doNotStart = true;
        ProcessHandler handler = getProcessHandler();
        if (handler != null) {
          handler.destroyProcess();
        }
      }

      @Override
      public boolean shouldStartInBackground() {
        return !modal;
      }

      @Override
      public boolean isConditionalModal() {
        return modal;
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (doNotStart || myProject == null || myProject.isDisposed()) {
          return;
        }
        indicator.setIndeterminate(true);
        consumer.consume(execute());
      }
    });
  }

  @Nullable
  public ProcessHandler getProcessHandler() {
    return myProcessHandler;
  }

  private void showNotification(@NotNull final String message, final NotificationType type) {
    ApplicationManager.getApplication().invokeLater(
        new Runnable() {
          @Override
          public void run() {
            String title = getPresentableName();
            Notifications.Bus.notify(TerraformConstants.EXECUTION_NOTIFICATION_GROUP.createNotification(title, message, type, null), myProject);
          }
        });
  }

  private void showOutput(@NotNull OSProcessHandler originalHandler, @NotNull HistoryProcessListener historyProcessListener) {
    if (myShowOutputOnError) {
      BaseOSProcessHandler outputHandler = new KillableColoredProcessHandler(originalHandler.getProcess(), null);
      RunContentExecutor runContentExecutor = new RunContentExecutor(myProject, outputHandler)
          .withTitle(getPresentableName())
          .withActivateToolWindow(myShowOutputOnError);
      Disposer.register(myProject, runContentExecutor);
      runContentExecutor.run();
      historyProcessListener.apply(outputHandler);
    }
    if (myShowNotificationsOnError) {
      showNotification("Failed to run", NotificationType.ERROR);
    }
  }

  @NotNull
  public GeneralCommandLine createCommandLine() throws ExecutionException {
    GeneralCommandLine commandLine = !myPtyDisabled && PtyCommandLine.isEnabled() ? new PtyCommandLine() : new GeneralCommandLine();
    commandLine.setExePath(ObjectUtils.notNull(myExePath));
    commandLine.getEnvironment().putAll(myExtraEnvironment);

    commandLine.withWorkDirectory(myWorkDirectory);
    commandLine.addParameters(myParameterList.getList());
    commandLine.withParentEnvironmentType(myParentEnvironmentType);
    commandLine.withCharset(CharsetToolkit.UTF8_CHARSET);
    return commandLine;
  }

  @NotNull
  private String getPresentableName() {
    return ObjectUtils.notNull(myPresentableName, "terraform");
  }
}