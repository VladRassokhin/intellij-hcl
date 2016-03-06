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
package org.intellij.plugins.hcl;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.BooleanFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class CompletionTestCase extends LightCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(CompletionTestCase.class);

  protected abstract String getFileName();

  protected abstract Language getExpectedLanguage();

  protected void doBasicCompletionTest(String text, Collection<String> expected) {
    doBasicCompletionTest(text, expected.size(), expected.toArray(new String[expected.size()]));
  }

  protected void doBasicCompletionTest(String text, final int expectedAllSize, final String... expected) {
    doBasicCompletionTest(text, getPartialMatcher(expectedAllSize, expected));
  }
  protected void doBasicCompletionTest(String text, final String... expected) {
    doBasicCompletionTest(text, getPartialMatcher(expected));
  }

  protected void doBasicCompletionTest(String text, BooleanFunction<Collection<String>> matcher) {
    final PsiFile psiFile = myFixture.configureByText(getFileName(), text);
    System.out.println("PsiFile = " + DebugUtil.psiToString(psiFile, true));
    assertEquals(getExpectedLanguage(), psiFile.getLanguage());
    final LookupElement[] elements = myFixture.complete(CompletionType.BASIC, 2);
    System.out.println("LookupElements = " + Arrays.toString(elements));
    final List<String> strings = myFixture.getLookupElementStrings();
    assertNotNull(strings);
    System.out.println("LookupStrings = " + strings);
    assertTrue("Matcher expected to return true", matcher.fun(strings));
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getPartialMatcher(final String... expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        assertContainsElements(strings, expectedPart);
        return true;
      }
    };
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getPartialMatcher(final Collection<String> expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        assertContainsElements(strings, expectedPart);
        return true;
      }
    };
  }

  @NotNull
  protected BooleanFunction<Collection<String>> getPartialMatcher(final int expectedAllSize, final String... expectedPart) {
    return new BooleanFunction<Collection<String>>() {
      @Override
      public boolean fun(Collection<String> strings) {
        assertContainsElements(strings, expectedPart);
        assertEquals("Actual lookup elements: " + strings, expectedAllSize, strings.size());
        return true;
      }
    };
  }
}
