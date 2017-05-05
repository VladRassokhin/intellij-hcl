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
package org.intellij.plugins.hcl.terraform.config.codeinsight;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.lang.Language;
import org.intellij.plugins.hcl.CompletionTestCase;
import org.intellij.plugins.hcl.terraform.config.TerraformLanguage;
import org.intellij.plugins.hcl.terraform.config.model.PropertyOrBlockType;
import org.intellij.plugins.hcl.terraform.config.model.TypeModel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

public abstract class TFBaseCompletionTestCase extends CompletionTestCase {
  public static final Collection<String> COMMON_RESOURCE_PROPERTIES = new TreeSet<String>(Collections2.transform(Arrays.asList(TypeModel.AbstractResource.getProperties()), new Function<PropertyOrBlockType, String>() {
    @Override
    public String apply(@SuppressWarnings("NullableProblems") @NotNull PropertyOrBlockType propertyOrBlockType) {
      return propertyOrBlockType.getName();
    }
  }));
  public static final Collection<String> COMMON_DATA_SOURCE_PROPERTIES = new TreeSet<String>(Collections2.transform(Arrays.asList(TypeModel.AbstractDataSource.getProperties()), new Function<PropertyOrBlockType, String>() {
    @Override
    public String apply(@SuppressWarnings("NullableProblems") @NotNull PropertyOrBlockType propertyOrBlockType) {
      return propertyOrBlockType.getName();
    }
  }));

  @Override
  protected String getTestDataPath() {
    return "tests/data";
  }

  @Override
  protected String getFileName() {
    return "a.tf";
  }

  @Override
  protected Language getExpectedLanguage() {
    return TerraformLanguage.INSTANCE;
  }
}
