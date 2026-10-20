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

import com.google.gson.annotations.JsonAdapter
import com.wultra.android.mtokensdk.api.operation.UtcInstantSerializer
import com.wultra.android.mtokensdk.log.WMTLogger
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Records user navigation through Pre-approval screens.
 * Each “visit” captures an opening timestamp and, when closed,
 * a closing timestamp and the action that ended the visit.
 *
 * Timestamps are captured with the device time. When the recorded data is sent with
 * `OperationsService.authorizeOperation` or `OperationsService.rejectOperation`, the SDK
 * aligns them to server time, synchronizing the time with the server first if needed.
 */
class PreApprovalScreensRecorder() : MobileTokenDataRecord {

    /**
     * Deprecated. Previously used [powerAuthSDK] as a source of synchronized time.
     *
     * The recorder now captures device time and the SDK aligns it to server time
     * during operation authorization or rejection. The [powerAuthSDK] parameter is ignored.
     *
     * @param powerAuthSDK Instance of PowerAuthSDK (no longer used).
     */
    @Deprecated(
        message = "PowerAuthSDK is no longer needed. The SDK aligns recorded timestamps to server time " +
            "during authorizeOperation and rejectOperation. Use PreApprovalScreensRecorder() instead.",
        replaceWith = ReplaceWith("PreApprovalScreensRecorder()")
    )
    constructor(@Suppress("UNUSED_PARAMETER") powerAuthSDK: PowerAuthSDK) : this()

    private companion object { const val KEY = "preApprovalScreens" }
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
        @JsonAdapter(UtcInstantSerializer::class)
        val timestampOpened: ZonedDateTime,
        @JsonAdapter(UtcInstantSerializer::class)
        var timestampClosed: ZonedDateTime? = null,
        var action: String? = null
    )

    /** Snapshot of visits with device-time timestamps. Aligned to server time before sending. */
    private class Visits(private val visits: List<Visit>) : AbstractList<Visit>(), ServerTimeAdjustable {
        override val size: Int get() = visits.size
        override fun get(index: Int): Visit = visits[index]

        override fun adjusted(localTimeAdjustment: Long): List<Visit> = visits.map {
            it.copy(
                timestampOpened = it.timestampOpened.plus(localTimeAdjustment, ChronoUnit.MILLIS),
                timestampClosed = it.timestampClosed?.plus(localTimeAdjustment, ChronoUnit.MILLIS)
            )
        }
    }

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

    /** Closes the current or last unclosed visit and records [action]. */
    fun end(id: String, action: Action) = apply {
        synchronized(mutex) {

            // Currently open visit matches this id
            openVisit?.let { live ->
                if (live.screen == id) {
                    live.timestampClosed = now()
                    live.action = action.name
                    visits += live
                    openVisit = null
                    return@apply
                }
            }

            // No openVisit, but last visit with same id has no timestampClosed and action
            val last = visits.lastOrNull()
            if (last != null && last.screen == id && (last.timestampClosed == null && last.action == null)) {
                last.timestampClosed = now()
                last.action = action.name
            }
        }
    }

    /** Reset collected visits. */
    fun reset() = apply {
        synchronized(mutex) {
            openVisit = null
            visits.clear()
        }
    }

    /**
     * Record value: snapshot of visits (a list) with device-time timestamps.
     *
     * The timestamps are aligned to server time by the SDK when the value is sent
     * as a top-level `mobileTokenData` entry during operation authorization or rejection.
     */
    override fun build(): Any = synchronized(mutex) {
        // If a visit is still open, close it now (no action)
        openVisit?.let { live ->
            WMTLogger.w("PreApprovalScreensRecorder is building unended visit for screen: ${live.screen}, ending it automatically with no action.")
            live.timestampClosed = now()
            visits += live
            openVisit = null
        }

        Visits(visits.map { it.copy() })
    }

    /** Current device time. Aligned to server time by the SDK before sending. */
    private fun now(): ZonedDateTime = ZonedDateTime.now()
}
