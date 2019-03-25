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
package org.intellij.plugins.hcl.terraform.config.model.version

import java.util.*
import java.util.function.BiFunction
import java.util.regex.Pattern

class VersionConstraint private constructor(val constraints: List<Constraint>) {
  data class Constraint(val operation: ConstraintFunction, val check: Version, val original: String) {
    fun check(version: Version): Boolean {
      return operation.apply(version, this.check)
    }

    override fun toString(): String {
      return original
    }
  }

  companion object {
    val AnyVersion by lazy { return@lazy parse(">=0.0.0") }

    @Throws(MalformedConstraintException::class)
    fun parse(source: String): VersionConstraint {
      return VersionConstraint(source.split(',').map {
        parseSingle(it)
      })
    }

    private val ops = mapOf(
        "" to ConstraintFunction.Equal,
        "=" to ConstraintFunction.Equal,
        "!=" to ConstraintFunction.NotEqual,
        ">" to ConstraintFunction.GreaterThan,
        "<" to ConstraintFunction.LessThan,
        ">=" to ConstraintFunction.GreaterThanEqual,
        "<=" to ConstraintFunction.LessThanEqual,
        "~>" to ConstraintFunction.Pessimistic
    )

    private val constraintRegexp = String.format("^\\s*(%s)\\s*(%s)\\s*\$", ops.keys.joinToString("|") { Pattern.quote(it) }, VersionRegexpRaw).toRegex()


    private fun parseSingle(s: String): Constraint {
      val match = constraintRegexp.matchEntire(s)?.groupValues ?: throw MalformedConstraintException("Malformed constraint: $s")
      val version = try {
        Version.parse(match[2])
      } catch (e: MalformedVersionException) {
        throw MalformedConstraintException(e.message!!)
      }
      val operation = ops[match[1]] ?: throw MalformedConstraintException("Unsupported operation: ${match[1]}")
      return Constraint(operation, version, s)
    }
  }

  fun check(version: Version): Boolean {
    return constraints.all { it.check(version) }
  }

  override fun toString(): String {
    return constraints.joinToString(",")
  }
}

class MalformedConstraintException(message: String) : Exception(message)

fun prereleaseCheck(v: Version, c: Version): Boolean {
  val vPre = v.pre.isNotEmpty()
  val cPre = c.pre.isNotEmpty()
  if (cPre && vPre) {
    // A constraint with a pre-release can only match a pre-release version
    // with the same base segments.
    return Arrays.equals(c.segments, v.segments)
  }
  if (!cPre && vPre) {
    // A constraint without a pre-release can only match a version without a
    // pre-release.
    return false
  }
  if (cPre && !vPre) {
    // OK, except with the pessimistic operator
  }
  if (!cPre && !vPre) {
    // OK
  }
  return true
}

sealed class ConstraintFunction(val name: String, private val f: (Version, Version) -> Boolean) : BiFunction<Version, Version, Boolean> {
  override fun apply(t: Version, u: Version): Boolean = f(t, u)

  object Equal : ConstraintFunction("=", { v, c -> v == c })
  object NotEqual : ConstraintFunction("!=", { v, c -> v != c })
  object GreaterThan : ConstraintFunction(">", { v, c -> prereleaseCheck(v, c) && v > c })
  object LessThan : ConstraintFunction("<", { v, c -> prereleaseCheck(v, c) && v < c })
  object GreaterThanEqual : ConstraintFunction(">=", { v, c -> prereleaseCheck(v, c) && v >= c })
  object LessThanEqual : ConstraintFunction("<=", { v, c -> prereleaseCheck(v, c) && v <= c })

  object Pessimistic : ConstraintFunction("~>", fun(v: Version, c: Version): Boolean {
    if (!prereleaseCheck(v, c) || (c.pre.isNotEmpty() && v.pre.isEmpty())) {
      // Using a pessimistic constraint with a pre-release, restricts versions to pre-releases
      return false
    }

    // If the version being checked is naturally less than the constraint, then there
    // is no way for the version to be valid against the constraint
    if (v < c) {
      return false
    }

    // If the version being checked has less specificity than the constraint, then there
    // is no way for the version to be valid against the constraint
    if (c.segments.size > v.segments.size) {
      return false
    }

    // Check the segments in the constraint against those in the version. If the version
    // being checked, at any point, does not have the same values in each index of the
    // constraints segments, then it cannot be valid against the constraint.
    for (i in 0..(c.si - 2)) {
      if (v.segments[i] != c.segments[i]) {
        return false
      }
    }

    // Check the last part of the segment in the constraint. If the version segment at
    // this index is less than the constraints segment at this index, then it cannot
    // be valid against the constraint
    if (c.segments[c.si - 1] > v.segments[c.si - 1]) {
      return false
    }

    // If nothing has rejected the version by now, it's valid
    return true
  })
}


