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

package com.wultra.android.mtokensdk.api.operation.model.mobiletokendata

import com.wultra.android.mtokensdk.api.operation.model.IOperation

/**
 * Helper for composing additional data passed with an [IOperation].
 *
 * The builder combines generic key–value pairs with structured records
 * that contribute a single top-level entry.
 */
object MobileTokenData {

    /**
     * Builds a map of additional data composed from generic entries
     * and structured [MobileTokenDataRecord] instances.
     *
     * Thread-safe: all mutations are synchronized.
     * Replacement semantics: putting the same key overwrites the prior value.
     */
    class Builder(
        /** Optional initial entries inserted into the builder. */
        initialData: Map<String, Any> = emptyMap<String, Any>()
    ) {
        /** Thread-safe backing map for mobileTokenData. */
        private val mobileTokenData = LinkedHashMap(initialData)

        /** Synchronization primitive guarding internal state. */
        private val mutex = Any()

        /** Adds or replaces a generic key–value entry. */
        fun put(key: String, value: Any) = apply {
            synchronized(mutex) { mobileTokenData[key] = value }
        }

        /** Adds or replaces a structured record under its declared [key]. */
        fun put(record: MobileTokenDataRecord) = apply {
            val value = record.build()
            put(record.key, value)
        }

        /** Removes an entry by its key. Returns true if removed. */
        fun remove(key: String): Boolean = synchronized(mutex) {
            mobileTokenData.remove(key) != null
        }

        /** Removes a record by its key. Returns true if removed. */
        fun remove(record: MobileTokenDataRecord): Boolean = remove(record.key)

        /** Removes all entries. */
        fun clear() = apply { synchronized(mutex) { mobileTokenData.clear() } }

        /**
         * Returns a snapshot of the collected data (copy).
         * Safe to assign to `operation.mobileTokenData`.
         */
        fun build(): Map<String, Any> = synchronized(mutex) {
            mobileTokenData.toMap()
        }
    }
}
