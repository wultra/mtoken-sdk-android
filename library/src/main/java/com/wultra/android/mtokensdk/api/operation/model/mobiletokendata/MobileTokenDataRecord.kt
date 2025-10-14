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
