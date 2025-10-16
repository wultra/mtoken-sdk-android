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

/**
 * Base class for a single top-level record contributing one key–value entry
 * to the final `mobileTokenData` map.
 *
 * Subclasses represent either:
 *  - a **finalized record** (immutable key/value pair), or
 *  - a **recorder** that collects data over time and finalizes itself in [build].
 */
abstract class MobileTokenDataRecord protected constructor(
    /** Reference to the parent builder this record belongs to. */
    protected open val dataBuilder: MobileTokenData.Builder
) {

    /** Key under which this record will be stored in `mobileTokenData`. */
    abstract val key: String

    /**
     * Clears internal state so the record can be reused.
     * Subclasses may override to implement their own reset logic.
     * The default implementation does nothing.
     */
    open fun reset() {}

    /**
     * Attaches this record to the parent builder.
     *
     * This method is **final** to ensure every record is properly
     * added to the parent builder before serialization.
     *
     * It is safe (and expected) to call [build] multiple times;
     * subsequent calls will replace the existing record entry.
     */
    fun build() {
        dataBuilder.put(this)
    }

    /**
     * Produces the record’s value object that will be serialized into
     * the final `mobileTokenData` map.
     *
     * Called automatically by [MobileTokenData.Builder.build].
     */
    abstract fun toValue(): Any
}
