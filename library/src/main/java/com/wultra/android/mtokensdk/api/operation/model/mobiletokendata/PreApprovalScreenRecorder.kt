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

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Helper used to document the user flow through Pre-approval Screens.
 *
 * The recorder tracks when each screen in the Pre-approval flow is opened
 * and closed, together with the user action that caused the transition.
 * Each recorded visit contains timestamps and an optional [ScreenCloseAction].
 *
 * When finalized via [build], the recorder produces a structured record
 * that can be attached to a [MobileTokenData.Builder] and later serialized
 * into `mobileTokenData` during operation authorization or rejection.
 */
class PreApprovalScreensRecorder(
    private val parent: MobileTokenData.Builder
) : MobileTokenDataRecord {
    companion object {
        /** Key under which this record is stored in the resulting map. */
        const val KEY = "preApprovalScreens"
    }

    /** Key of this record. */
    override val key: String get() = KEY

    /** Action type recorded when a screen visit is closed. */
    sealed class Action(val name: String) {
        object CONTINUE : Action("CONTINUE")
        object BACK : Action("BACK")
        object CLOSE : Action("CLOSE")
        object REJECT : Action("REJECT")
        object SCAN : Action("SCAN")

        // Custom user-defined action
        class Custom(action: String) : Action(action)
    }

    /** Represents a single visit entry. */
    private data class Visit(
        val screen: String,
        val opened: ZonedDateTime,
        var closed: ZonedDateTime? = null,
        var action: Action? = null
    )

    private var open: Visit? = null
    private val finalized = mutableListOf<Visit>()
    private var sealed = false
    private var snapshot: List<Map<String, Any>>? = null // frozen payload after build()

    /** Mutex used to synchronize access to visit records. */
    private val mutex = Any()

    /**
     * Opens a new visit entry for the specified [id].
     * If another visit is already open, it is finalized first without closed and action.
     */
    fun begin(id: String) = apply {
        synchronized(mutex) {
            if (sealed || id.isEmpty()) return@apply
            open?.let { live ->
                if (live.screen == id) return@apply
                finalized += live
            }
            open = Visit(id, now())
        }
    }

    /**
     * Closes the current visit with the specified [action].
     * If [id] does not match the currently open screen, the call has no effect.
     */
    fun end(id: String, action: Action) = apply {
        synchronized(mutex) {
            if (sealed) return@apply
            val live = open ?: return@apply
            if (live.screen != id) return@apply
            live.closed = now()
            live.action = action
            finalized += live
            open = null
        }
    }

    /**
     * Closes any open visit and marks it with the given [action].
     * Intended for cases where the flow ends unexpectedly.
     */
    fun closeOpenAs(action: Action) = apply {
        synchronized(mutex) {
            if (sealed) return@apply
            val live = open ?: return@apply
            live.closed = now()
            live.action = action
            finalized += live
            open = null
        }
    }

    /** Returns the finalized list of recorded visits. */
    override fun toValue(): Any = synchronized(mutex) { snapshot ?: emptyList() }

    /**
     * Converts recorded data into a serializable format and
     * adds this record to the parent builder.
     */
    override fun build() {
        synchronized(mutex) {
            if (sealed) return
            sealed = true
            snapshot = finalized.map { v ->
                buildMap {
                    put("screen", v.screen)
                    put("timestampOpened", v.opened)
                    v.closed?.let { put("timestampClosed", it) }
                    v.action?.let { put("action", it.name) }
                }
            }
        }
        parent.put(this) // attach self as the finalized record
    }

    /** Clears collected visits and allows the recorder to be used again. */
    override fun reset() {
        synchronized(mutex) {
            open = null
            finalized.clear()
            snapshot = null
            sealed = false
        }
    }

    /** Returns PowerAuthSDK synchronized current date-time using the system clock */
    private fun now(): ZonedDateTime {
        val ts = parent.powerAuthSDK.timeSynchronizationService
        return if (ts.isTimeSynchronized) {
            ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(ts.currentTime),
                ZoneId.systemDefault()
            )
        } else ZonedDateTime.now()
    }
}
