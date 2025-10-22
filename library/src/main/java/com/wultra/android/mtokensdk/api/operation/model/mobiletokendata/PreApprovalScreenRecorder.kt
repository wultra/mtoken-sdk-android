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
import io.getlime.security.powerauth.sdk.PowerAuthSDK

/**
 * Records user navigation through Pre-approval screens.
 * Each “visit” captures an opening timestamp and, when closed,
 * a closing timestamp and the action that ended the visit.
 * Timestamps use PowerAuth time sync when available.
 */
class PreApprovalScreensRecorder(
    /** Time source (used only for synchronized timestamps). */
    private val powerAuthSDK: PowerAuthSDK
) : MobileTokenDataRecord {

    companion object { const val KEY = "preApprovalScreens" }
    override val key: String get() = KEY

    /** Extensible action set (serialized via [name]). */
    sealed class Action(val name: String) {
        object CONTINUE : Action("CONTINUE")
        object BACK : Action("BACK")
        object CLOSE : Action("CLOSE")
        object REJECT : Action("REJECT")
        object SCAN : Action("SCAN")
        class Custom(action: String) : Action(action)
    }

    /** One screen visit within the Pre-approval flow. */
    private data class Visit(
        val screen: String,
        val timestampOpened: ZonedDateTime,
        var timestampClosed: ZonedDateTime? = null,
        var action: String? = null
    )

    /** Synchronization primitive that protects recorder state. */
    private val mutex = Any()
    /** Currently open visit (if any). */
    private var openVisit: Visit? = null

    /** Finalized visits accumulated by the recorder. */
    private val visits = mutableListOf<Visit>()

    /** Open a visit for [id]. If another is open, append it as-is (no close/action). */
    fun begin(id: String) = apply {
        if (id.isEmpty()) return@apply
        synchronized(mutex) {
            openVisit?.let { live ->
                if (live.screen == id) return@apply
                visits += live
            }
            openVisit = Visit(id, now())
        }
    }

    /** Close the current visit (if id matches) and record [action]. */
    fun end(id: String, action: Action) = apply {
        synchronized(mutex) {
            val live = openVisit ?: return@apply
            if (live.screen != id) return@apply
            live.timestampClosed = now()
            live.action = action.name
            visits += live
            openVisit = null
        }
    }

    /** Reset collected visits. */
    fun reset() = apply {
        synchronized(mutex) {
            openVisit = null
            visits.clear()
        }
    }

    /** Record value: snapshot of visits. */
    override fun build(): Any = synchronized(mutex) { visits.toList() }

    /** PowerAuth-synchronized current time (fallback: system clock). */
    private fun now(): ZonedDateTime {
        val ts = powerAuthSDK.timeSynchronizationService
        return if (ts.isTimeSynchronized) {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts.currentTime), ZoneId.systemDefault())
        } else ZonedDateTime.now()
    }
}
