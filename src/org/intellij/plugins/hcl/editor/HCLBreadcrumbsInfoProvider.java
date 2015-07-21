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
package org.intellij.plugins.hcl.editor;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.xml.breadcrumbs.BreadcrumbsInfoProvider;
import org.intellij.plugins.hcl.HCLLanguage;
import org.intellij.plugins.hcl.psi.HCLBlock;
import org.intellij.plugins.hcl.psi.HCLProperty;
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HCLBreadcrumbsInfoProvider extends BreadcrumbsInfoProvider {
  @Override
  public Language[] getLanguages() {
    return new Language[]{HCLLanguage.INSTANCE$, TerraformLanguage.INSTANCE$};
  }

  @Override
  public boolean acceptElement(@NotNull PsiElement e) {
    return e instanceof HCLBlock || e instanceof HCLProperty;
  }

  @NotNull
  @Override
  public String getElementInfo(@NotNull PsiElement e) {
    if (e instanceof HCLBlock) {
      return ((HCLBlock) e).getName();
    }
    if (e instanceof HCLProperty) {
      return ((HCLProperty) e).getName();
    }
    throw new AssertionError("Only HCLBlock and HCLProperty supported, actual is " + e.getClass().getName());
  }

  @Nullable
  @Override
  public String getElementTooltip(@NotNull PsiElement e) {
    return null;
  }
}
