/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

package org.intellij.plugins.hcl.terraform.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.ExceptionUtil;
import org.intellij.plugins.hcl.HCLFileType;
import org.intellij.plugins.hcl.terraform.TerraformConstants;
import org.intellij.plugins.hcl.terraform.config.TerraformFileType;
import org.intellij.plugins.hcl.terraform.util.TFExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public abstract class TFExternalToolsAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(TFExternalToolsAction.class);

  private static void error(@NotNull String title, @NotNull Project project, @Nullable Exception ex) {
    String message = ex == null ? "" : ExceptionUtil.getUserStackTrace(ex, LOG);
    NotificationType type = NotificationType.ERROR;
    Notifications.Bus.notify(TerraformConstants.EXECUTION_NOTIFICATION_GROUP.createNotification(title, message, type, null), project);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null || !file.isInLocalFileSystem() || !isAvailableOnFile(file)) {
      e.getPresentation().setEnabled(false);
      return;
    }
    e.getPresentation().setEnabled(true);
  }

  protected boolean isAvailableOnFile(VirtualFile file) {
    return file.getFileType() == HCLFileType.INSTANCE || file.getFileType() == TerraformFileType.INSTANCE;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
    assert project != null;
    String title = StringUtil.notNullize(e.getPresentation().getText());

    Module module = ModuleUtilCore.findModuleForFile(file, project);
    try {
      doSomething(file, module, project, title);
    } catch (ExecutionException ex) {
      error(title, project, ex);
      LOG.error(ex);
    }
  }

  protected boolean doSomething(@NotNull VirtualFile virtualFile,
                                @Nullable Module module,
                                @NotNull Project project,
                                @NotNull String title) throws ExecutionException {
    return doSomething(virtualFile, module, project, title, false);
  }

  private boolean doSomething(@NotNull VirtualFile virtualFile,
                              @Nullable Module module,
                              @NotNull Project project,
                              @NotNull String title,
                              boolean withProgress) {
    //noinspection unchecked
    return doSomething(virtualFile, module, project, title, withProgress, Consumer.EMPTY_CONSUMER);
  }

  protected boolean doSomething(@NotNull final VirtualFile virtualFile,
                                @Nullable Module module,
                                @NotNull Project project,
                                @NotNull String title,
                                boolean withProgress,
                                @NotNull final Consumer<Boolean> consumer) {
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (document != null) {
      FileDocumentManager.getInstance().saveDocument(document);
    } else {
      FileDocumentManager.getInstance().saveAllDocuments();
    }

    createExecutor(project, module, title, virtualFile).executeWithProgress(withProgress,
        aBoolean -> {
          consumer.consume(aBoolean);
          VfsUtil.markDirtyAndRefresh(true, true, true, virtualFile);
        });
    return true;
  }

  protected TFExecutor createExecutor(@NotNull Project project,
                                      @Nullable Module module,
                                      @NotNull String title,
                                      @NotNull VirtualFile virtualFile) {
    String filePath = virtualFile.getCanonicalPath();
    assert filePath != null;
    return createExecutor(project, module, title, filePath);
  }

  @NotNull
  protected abstract TFExecutor createExecutor(@NotNull Project project,
                                               @Nullable Module module,
                                               @NotNull String title,
                                               @NotNull String filePath);
}