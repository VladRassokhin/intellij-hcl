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
package org.intellij.plugins.hil.refactoring;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.EditorComboBoxEditor;
import com.intellij.ui.EditorComboBoxRenderer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.StringComboboxEditor;
import org.intellij.plugins.hcl.HCLBundle;
import org.intellij.plugins.hcl.terraform.config.TerraformFileType;
import org.intellij.plugins.hil.psi.ILExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.util.Collection;

/**
 * User: Fedor.Korotkov
 */
public class ILIntroduceDialog extends DialogWrapper {
  private JPanel myContentPane;
  private JLabel myNameLabel;
  private ComboBox myNameComboBox;
  private JCheckBox myReplaceAll;

  private final Project myProject;
  private final int myOccurrencesCount;
  private final IntroduceValidator myValidator;
  private final ILExpression myExpression;

  public ILIntroduceDialog(@NotNull final Project project,
                           @NotNull final String caption,
                           @NotNull IntroduceValidator validator,
                           final IntroduceOperation operation) {
    super(project, true);
    myOccurrencesCount = operation.getOccurrences().size();
    myValidator = validator;
    myProject = project;
    myExpression = operation.getInitializer();
    setUpNameComboBox(operation.getSuggestedNames());

    setTitle(caption);
    init();
    setupDialog();
    updateControls();
  }

  private void setUpNameComboBox(Collection<String> possibleNames) {
    final EditorComboBoxEditor comboEditor = new StringComboboxEditor(myProject, TerraformFileType.INSTANCE, myNameComboBox);

    myNameComboBox.setEditor(comboEditor);
    myNameComboBox.setRenderer(new EditorComboBoxRenderer(comboEditor));
    myNameComboBox.setEditable(true);
    myNameComboBox.setMaximumRowCount(8);

    myNameComboBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        updateControls();
      }
    });
    ((EditorTextField)myNameComboBox.getEditor().getEditorComponent()).addDocumentListener(new DocumentListener() {
      public void beforeDocumentChange(DocumentEvent event) {
      }

      public void documentChanged(DocumentEvent event) {
        updateControls();
      }
    });

    myContentPane.registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
            new Runnable() {
              @Override
              public void run() {
                IdeFocusManager.getGlobalInstance().requestFocus(myNameComboBox, true);
              }
            });
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

    for (String possibleName : possibleNames) {
      myNameComboBox.addItem(possibleName);
    }
  }

  private void setupDialog() {
    myReplaceAll.setMnemonic(KeyEvent.VK_A);
    myNameLabel.setLabelFor(myNameComboBox);

    // Replace occurrences check box setup
    if (myOccurrencesCount > 1) {
      myReplaceAll.setSelected(false);
      myReplaceAll.setEnabled(true);
      myReplaceAll.setText(myReplaceAll.getText() + " (" + myOccurrencesCount + " occurrences)");
    } else {
      myReplaceAll.setSelected(false);
      myReplaceAll.setEnabled(false);
    }
  }

  public JComponent getPreferredFocusedComponent() {
    return myNameComboBox;
  }

  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  @Nullable
  public String getName() {
    final Object item = myNameComboBox.getEditor().getItem();
    if ((item instanceof String) && ((String) item).length() > 0) {
      return ((String) item).trim();
    }
    return null;
  }

  public Project getProject() {
    return myProject;
  }

  public ILExpression getExpression() {
    return myExpression;
  }

  public boolean doReplaceAllOccurrences() {
    return myReplaceAll.isSelected();
  }

  private void updateControls() {
    final String name = getName();
    boolean nameValid = myValidator.isNameValid(name, getProject());
    setErrorText(!nameValid ? HCLBundle.message("refactoring.introduce.name.error") : null);
    nameValid &= name != null && myValidator.checkPossibleName(name, myExpression);
    setErrorText(!nameValid ? HCLBundle.message("refactoring.introduce.variable.scope.error") : null);
    setOKActionEnabled(nameValid);
  }

}
