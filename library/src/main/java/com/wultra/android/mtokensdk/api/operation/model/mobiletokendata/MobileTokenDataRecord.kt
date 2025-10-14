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

import com.wultra.android.mtokensdk.api.operation.model.mobiletokendata.MobileTokenData.Builder

/**
 * A single top-level record that contributes one key-value entry
 * to the final `mobileTokenData` map.
 *
 * Implementations may represent either:
 *  - a **finalized record** (immutable key/value pair), or
 *  - a **recorder** that collects data over time and finalizes it in [build].
 */
interface MobileTokenDataRecord {

    /** Key under which this record will be stored in `mobileTokenData`. */
    val key: String

    /** Returns the value object that will be serialized into the final map. */
    fun toValue(): Any

    /**
     * Finalizes the record. For recorders, this should capture any
     * collected data, freeze internal state, and attach itself to
     * the parent [Builder] via [Builder.put]. This method must be
     * idempotent.
     */
    fun build()

    /** Clears internal state so the record can be built again. */
    fun reset()
}
