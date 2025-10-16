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
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import io.getlime.security.powerauth.sdk.PowerAuthSDK

/**
 * Helper used to document the user flow through [PreApprovalScreen]s.
 *
 * The recorder tracks when each screen in the Pre-approval flow is opened
 * and closed, together with the user action that caused the transition.
 * Each recorded visit contains timestamps and an optional [Action].
 *
 * When finalized via [build], the recorder produces a structured record
 * that can be attached to a [MobileTokenData.Builder] and later serialized
 * into `mobileTokenData` during operation authorization or rejection.
 */
class PreApprovalScreensRecorder(
    private val powerAuthSDK: PowerAuthSDK,
    override val dataBuilder: MobileTokenData.Builder
) : MobileTokenDataRecord(dataBuilder) {
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
        val timestampOpened: ZonedDateTime,
        var timestampClosed: ZonedDateTime? = null,
        var action: Action? = null
    )

    private var openVisit: Visit? = null
    private val visits = mutableListOf<Visit>()
    private var sealed = false

    /** Mutex used to synchronize access to visit records. */
    private val mutex = Any()

    /**
     * Starts a new visit entry for the specified [id].
     * If another visit is already open, it is finalized first without closed and action.
     */
    fun begin(id: String) = apply {
        synchronized(mutex) {
            if (sealed || id.isEmpty()) return@apply
            openVisit?.let { live ->
                if (live.screen == id) return@apply
                visits += live
            }
            openVisit = Visit(id, now())
        }
    }

    /**
     * Ends the current visit with the specified [action].
     * If [id] does not match the currently open screen, the call has no effect.
     */
    fun end(id: String, action: Action) = apply {
        synchronized(mutex) {
            if (sealed) return@apply
            val live = openVisit ?: return@apply
            if (live.screen != id) return@apply
            live.timestampClosed = now()
            live.action = action
            visits += live
            openVisit = null
        }
    }

    /** Clears collected visits and allows the recorder to be used again. */
    override fun reset() {
        synchronized(mutex) {
            openVisit = null
            visits.clear()
            sealed = false
        }
    }

    /**
     * Returns the list of visits.
     * This call marks the record as sealed, preventing further modifications.
     */
    override fun toValue(): Any {
        synchronized(mutex) {
            sealed = true
            return visits.map { v ->
                buildMap {
                    put("screen", v.screen)
                    put("timestampOpened", v.timestampOpened)
                    v.timestampClosed?.let { put("timestampClosed", it) }
                    v.action?.let { put("action", it.name) }
                }
            }
        }
    }

    /** Returns PowerAuthSDK synchronized current date-time using the system clock */
    private fun now(): ZonedDateTime {
        val ts = powerAuthSDK.timeSynchronizationService
        return if (ts.isTimeSynchronized) {
            ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(ts.currentTime),
                ZoneId.systemDefault()
            )
        } else ZonedDateTime.now()
    }
}
