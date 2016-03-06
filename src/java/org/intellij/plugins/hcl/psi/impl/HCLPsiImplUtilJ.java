/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.psi.impl;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import org.intellij.plugins.hcl.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HCLPsiImplUtilJ {
  @NotNull
  public static String getName(@NotNull HCLProperty property) {
    return HCLPsiImplUtils.INSTANCE.getName(property);
  }

  @NotNull
  public static String getName(@NotNull HCLBlock block) {
    return HCLPsiImplUtils.INSTANCE.getName(block);
  }

  @NotNull
  public static String getFullName(@NotNull HCLBlock block) {
    return HCLPsiImplUtils.INSTANCE.getFullName(block);
  }

  @NotNull
  public static HCLValue getNameElement(@NotNull HCLProperty property) {
    return HCLPsiImplUtils.INSTANCE.getNameElement(property);
  }

  @NotNull
  public static HCLElement[] getNameElements(@NotNull HCLBlock block) {
    return HCLPsiImplUtils.INSTANCE.getNameElements(block);
  }

  @Nullable
  public static HCLValue getValue(@NotNull HCLProperty property) {
    return HCLPsiImplUtils.INSTANCE.getValue(property);
  }

  @Nullable
  public static HCLObject getObject(@NotNull HCLBlock block) {
    return HCLPsiImplUtils.INSTANCE.getObject(block);
  }

  public static boolean isQuotedString(@NotNull HCLLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.isQuotedString(literal);
  }

  @Nullable
  public static ItemPresentation getPresentation(@NotNull HCLProperty property) {
    return HCLPsiImplUtils.INSTANCE.getPresentation(property);
  }

  @Nullable
  public static ItemPresentation getPresentation(@NotNull HCLBlock block) {
    return HCLPsiImplUtils.INSTANCE.getPresentation(block);
  }

  @Nullable
  public static ItemPresentation getPresentation(@NotNull HCLArray array) {
    return HCLPsiImplUtils.INSTANCE.getPresentation(array);
  }

  @Nullable
  public static ItemPresentation getPresentation(@NotNull HCLObject o) {
    return HCLPsiImplUtils.INSTANCE.getPresentation(o);
  }

  @NotNull
  public static List<Pair<TextRange, String>> getTextFragments(@NotNull HCLStringLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.getTextFragments(literal);
  }

  @Nullable
  public static HCLProperty findProperty(@NotNull HCLObject object, @NotNull String name) {
    return HCLPsiImplUtils.INSTANCE.findProperty(object, name);
  }

  @NotNull
  public static String getValue(@NotNull HCLStringLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.getValue(literal);
  }

  public static char getQuoteSymbol(@NotNull HCLStringLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.getQuoteSymbol(literal);
  }

  @NotNull
  public static String getValue(@NotNull HCLHeredocLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.getValue(literal);
  }

  @NotNull
  public static String getValue(@NotNull HCLHeredocContent content) {
    return HCLPsiImplUtils.INSTANCE.getValue(content);
  }

  @NotNull
  public static List<String> getLines(@NotNull HCLHeredocContent content) {
    return HCLPsiImplUtils.INSTANCE.getLines(content);
  }

  public static int getLinesCount(@NotNull HCLHeredocContent content) {
    return HCLPsiImplUtils.INSTANCE.getLinesCount(content);
  }

  @NotNull
  public static String getName(@NotNull HCLHeredocMarker marker) {
    return HCLPsiImplUtils.INSTANCE.getName(marker);
  }

  public static boolean getValue(@NotNull HCLBooleanLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.getValue(literal);
  }

  public static double getValue(@NotNull HCLNumberLiteral literal) {
    return HCLPsiImplUtils.INSTANCE.getValue(literal);
  }

  @NotNull
  public static String getId(@NotNull HCLIdentifier identifier) {
    return HCLPsiImplUtils.INSTANCE.getId(identifier);
  }
}
