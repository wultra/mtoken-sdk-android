/*
 * Copyright 2025 Wultra s.r.o.
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
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.wultra.android.mtokensdk

/**
 * Lazy loaded instance with possibility of "peek".
 */
class Lazy<T>(private val factory: () -> T) {

    @Volatile
    private var instance: T? = null

    /**
     * Returns the instance, initializing it if necessary.
     */
    val lazy: T
        get() {
            return instance ?: synchronized(this) {
                instance ?: factory().also { instance = it }
            }
        }

    /**
     * Returns the instance if initialized, otherwise returns null.
     */
    val optional: T?
        get() = instance
}
