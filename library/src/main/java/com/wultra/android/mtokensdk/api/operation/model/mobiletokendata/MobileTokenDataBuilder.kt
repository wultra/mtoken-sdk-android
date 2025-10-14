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

import io.getlime.security.powerauth.sdk.PowerAuthSDK
import kotlin.apply
import kotlin.reflect.KClass

/**
 * Helper for building additional data passed to the [com.wultra.android.mtokensdk.api.operation.model.IOperation].
 *
 * The container allows combining generic key–value pairs with structured
 * records that define their own key and value representation.
 */
object MobileTokenData {


    /**
     * Builds a map of additional data composed from generic entries
     * and structured [MobileTokenDataRecord] instances.
     */
    class Builder(
        internal val powerAuthSDK: PowerAuthSDK,
        base: Map<String, Any>? = null
    ) {

        /** Base content used as the initial state of the builder. */
        private val baseMap = LinkedHashMap<String, Any>().apply { if (base != null) putAll(base) }

        /** Generic key–value entries added directly by the application. */
        private val generic = LinkedHashMap<String, Any>()

        /** Finalized record objects to be written into the output map. */
        private val records = mutableListOf<MobileTokenDataRecord>()

        /** Cached helper instances keyed by their class type. */
        val helpers = mutableMapOf<KClass<out MobileTokenDataRecord>, MobileTokenDataRecord>()

        /** Mutex used to synchronize access to mutable internal state. */
        val mutex = Any()

        /**
         * Adds or replaces a generic key–value entry.
         * If a key already exists, its value is replaced.
         */
        fun put(key: String, value: Any) = apply {
            synchronized(mutex) {
                generic[key] = value
            }
        }

        /**
         * Adds or replaces a finalized [MobileTokenDataRecord].
         * If another record with the same [MobileTokenDataRecord.key] exists, it is replaced.
         */
        fun put(mobileTokenDataRecord: MobileTokenDataRecord) = apply {
            synchronized(mutex) {
                val i = records.indexOfFirst { it.key == mobileTokenDataRecord.key }
                if (i >= 0) records.removeAt(i)
                records += mobileTokenDataRecord
            }
        }

        /**
         * Returns an existing helper instance of type [T] or creates a new one
         * using [factory] if none is cached.
         */
        inline fun <reified T : MobileTokenDataRecord> helper(noinline factory: () -> T): T {
            val k = T::class
            synchronized(mutex) {
                @Suppress("UNCHECKED_CAST")
                return (helpers[k] as? T) ?: factory().also { helpers[k] = it }
            }
        }

        /**
         * Creates and returns the final immutable data map containing
         * all base entries, generic entries, and finalized records.
         */
        fun build(): Map<String, Any> = synchronized(mutex) {
            LinkedHashMap<String, Any>(baseMap.size + generic.size + records.size).apply {
                putAll(baseMap)
                putAll(generic)
                for (r in records) put(r.key, r.toValue())
            }
        }

        /** Removes a finalized record by key. Returns true if removed. */
        fun removeRecord(key: String): Boolean = synchronized(mutex) {
            val i = records.indexOfFirst { it.key == key }
            if (i >= 0) {
                records.removeAt(i)
                return true
            } else {
                return false
            }
        }

        /** Removes the given record (by its key). Returns true if removed. */
        fun remove(record: MobileTokenDataRecord): Boolean = removeRecord(record.key)

        /**
         * Removes the record and calls its reset() to clear internal state,
         * keeping any cached helper instance reusable.
         */
        fun reset(record: MobileTokenDataRecord) = apply {
            synchronized(mutex) {
                remove(record)
                record.reset()
            }
        }

        /** Removes all finalized records. */
        fun clearAllRecords() = apply {
            synchronized(mutex) {
                records.clear()
            }
        }
    }


    /**
     * Returns a [PreApprovalScreensRecorder] helper instance associated with
     * this [Builder]. The instance is created on first access and cached.
     */
    fun MobileTokenData.Builder.preApproval(): PreApprovalScreensRecorder = helper { PreApprovalScreensRecorder(this) }
}