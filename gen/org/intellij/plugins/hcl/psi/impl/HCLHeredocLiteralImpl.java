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

// This is a generated file. Not intended for manual editing.
package org.intellij.plugins.hcl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.intellij.plugins.hcl.HCLElementTypes.*;
import org.intellij.plugins.hcl.psi.*;

public class HCLHeredocLiteralImpl extends HCLLiteralImpl implements HCLHeredocLiteral {

  public HCLHeredocLiteralImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HCLElementVisitor visitor) {
    visitor.visitHeredocLiteral(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HCLElementVisitor) accept((HCLElementVisitor)visitor);
    else super.accept(visitor);
  }

  @NotNull
  public String getValue() {
    return HCLPsiImplUtilJ.getValue(this);
  }

  @Override
  @NotNull
  public HCLHeredocContent getContent() {
    return findNotNullChildByClass(HCLHeredocContent.class);
  }

  @Override
  @NotNull
  public HCLHeredocMarker getMarkerStart() {
    List<HCLHeredocMarker> p1 = PsiTreeUtil.getChildrenOfTypeAsList(this, HCLHeredocMarker.class);
    return p1.get(0);
  }

  @Override
  @Nullable
  public HCLHeredocMarker getMarkerEnd() {
    List<HCLHeredocMarker> p1 = PsiTreeUtil.getChildrenOfTypeAsList(this, HCLHeredocMarker.class);
    return p1.size() < 2 ? null : p1.get(1);
  }

  public boolean isIndented() {
    return HCLPsiImplUtilJ.isIndented(this);
  }

  @Nullable
  public Integer getIndentation() {
    return HCLPsiImplUtilJ.getIndentation(this);
  }

}
