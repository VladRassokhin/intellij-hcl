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
package org.intellij.plugins.hcl.formatter;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ListCellRendererWrapper;
import org.intellij.plugins.hcl.HCLFileType;
import org.intellij.plugins.hcl.HCLLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author Mikhail Golubev
 */
public class HCLCodeStylePanel extends CodeStyleAbstractPanel {
  public static final String ALIGNMENT_SAMPLE = "simple = true\n" +
      "po.int = false\n" +
      "under_score = 1\n" +
      "mi-nus = 'yep'\n" +
      "_5 = true\n" +
      "w1th.num8er5 = 'acceptable'";

  private JComboBox myPropertiesAlignmentCombo;
  private JPanel myPreviewPanel;
  private JPanel myPanel;

  @SuppressWarnings("unchecked")
  public HCLCodeStylePanel(@NotNull CodeStyleSettings settings) {
    super(HCLLanguage.INSTANCE$, null, settings);
    addPanelToWatch(myPanel);
    installPreviewPanel(myPreviewPanel);

    // Initialize combo box with property value alignment types
    for (HCLCodeStyleSettings.PropertyAlignment alignment : HCLCodeStyleSettings.PropertyAlignment.values()) {
      myPropertiesAlignmentCombo.addItem(alignment);
    }
    myPropertiesAlignmentCombo.setRenderer(new ListCellRendererWrapper<HCLCodeStyleSettings.PropertyAlignment>() {
      @Override
      public void customize(JList list, HCLCodeStyleSettings.PropertyAlignment value, int index, boolean selected, boolean hasFocus) {
        setText(value.getDescription());
      }
    });
    myPropertiesAlignmentCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          somethingChanged();
        }
      }
    });
  }

  @Override
  protected int getRightMargin() {
    return 80;
  }

  @Nullable
  @Override
  protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
    return EditorHighlighterFactory.getInstance().createEditorHighlighter(new LightVirtualFile("a.hcl"), scheme, null);
  }

  @NotNull
  @Override
  protected FileType getFileType() {
    return HCLFileType.INSTANCE$;
  }

  @Nullable
  @Override
  protected String getPreviewText() {
    return ALIGNMENT_SAMPLE;
  }

  @Override
  public void apply(CodeStyleSettings settings) throws ConfigurationException {
    getCustomSettings(settings).PROPERTY_ALIGNMENT = (getSelectedAlignmentType().getId());
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    return getCustomSettings(settings).PROPERTY_ALIGNMENT != getSelectedAlignmentType().getId();
  }

  @Nullable
  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  protected void resetImpl(CodeStyleSettings settings) {
    for (int i = 0; i < myPropertiesAlignmentCombo.getItemCount(); i++) {
      if (((HCLCodeStyleSettings.PropertyAlignment) myPropertiesAlignmentCombo.getItemAt(i)).getId() == getCustomSettings(settings).PROPERTY_ALIGNMENT) {
        myPropertiesAlignmentCombo.setSelectedIndex(i);
        break;
      }
    }
  }

  @NotNull
  private HCLCodeStyleSettings.PropertyAlignment getSelectedAlignmentType() {
    return (HCLCodeStyleSettings.PropertyAlignment) myPropertiesAlignmentCombo.getSelectedItem();
  }

  @NotNull
  private HCLCodeStyleSettings getCustomSettings(@NotNull CodeStyleSettings settings) {
    return settings.getCustomSettings(HCLCodeStyleSettings.class);
  }
}
