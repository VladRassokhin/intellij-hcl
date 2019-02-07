/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package org.intellij.plugins.hcl.terraform.config

import com.intellij.testFramework.deleteFile
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.impl.IdeaTestFixtureFactoryImpl
import junit.framework.TestCase

class TerraformDirFinderTest : CodeInsightFixtureTestCase<IdeaTestFixtureFactoryImpl.MyEmptyModuleFixtureBuilderImpl>() {

  fun testNone() {
    TestCase.assertNull(doFind("/"))
  }

  fun testItsAFile() {
    myFixture.tempDirFixture.createFile("/.terraform")
    TestCase.assertNull(doFind("/"))
  }

  fun testInSameDir() {
    val path = "/.terraform"
    findOrCreateDir(path)
    checkFind("/", path)
  }

  fun testInParentDir() {
    val path = "/.terraform"
    findOrCreateDir(path)
    checkFind("/a", path)
  }

  fun testCreatedThenDeleted() {
    val path1 = "/.terraform"
    val path2 = "/a/.terraform"
    val path3 = "/a/b/c/d/.terraform"
    val searchFrom = "/a/b/c/d"

    val dir1 = findOrCreateDir(path1)
    checkFind(searchFrom, path1)

    val dir2 = findOrCreateDir(path2)
    checkFind(searchFrom, path2)

    val dir3 = findOrCreateDir(path3)
    checkFind(searchFrom, path3)

    deleteFile(dir3.virtualFile)
    checkFind(searchFrom, path2)

    val dir3b = findOrCreateDir(path3)
    checkFind(searchFrom, path3)

    deleteFile(dir3b.virtualFile)
    deleteFile(dir2.virtualFile)
    checkFind(searchFrom, path1)

    deleteFile(dir1.virtualFile)
    TestCase.assertNull(doFind(searchFrom))
  }

  private fun doFind(searchFrom: String) =
      TerraformDirFinder.findTerraformDir(findOrCreateDir(searchFrom))

  private fun checkFind(searchFrom: String, expected: String) =
      TestCase.assertEquals("${myFixture.tempDirPath}$expected", doFind(searchFrom)?.virtualFile?.path)

  private fun findOrCreateDir(path: String) =
      myFixture.psiManager.findDirectory(myFixture.tempDirFixture.findOrCreateDir(path))!!
}
