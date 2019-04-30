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
@file:Suppress("LocalVariableName", "ClassName")

package org.intellij.plugins

import java.io.Serializable

interface ResettableLazy<out T> : Lazy<T> {
  fun reset()
}

fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> = ResettableSynchronizedLazy(initializer)

private object UNINITIALIZED_VALUE

private class ResettableSynchronizedLazy<out T>(initializer: () -> T, lock: Any? = null) : ResettableLazy<T>, Serializable {
  private var initializer: (() -> T)? = initializer
  @Volatile
  private var _value: Any? = UNINITIALIZED_VALUE
  // final field is required to enable safe publication of constructed instance
  private val lock = lock ?: this

  override fun reset() {
    synchronized(lock) {
      _value = UNINITIALIZED_VALUE
    }
  }

  override val value: T
    get() {
      val _v1 = _value
      if (_v1 !== UNINITIALIZED_VALUE) {
        @Suppress("UNCHECKED_CAST")
        return _v1 as T
      }

      return synchronized(lock) {
        val _v2 = _value
        if (_v2 !== UNINITIALIZED_VALUE) {
          @Suppress("UNCHECKED_CAST") (_v2 as T)
        } else {
          val typedValue = initializer!!()
          _value = typedValue
          typedValue
        }
      }
    }

  override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

  override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value is not initialized."
}