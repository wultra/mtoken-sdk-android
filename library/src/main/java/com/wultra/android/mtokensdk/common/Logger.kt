/*
 * Copyright 2022 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.common

import android.util.Log

/* ktlint-disable indent */

/**
 * Logger provides simple logging facility.
 *
 * Logs are written with "WMT" tag to standard [android.util.Log] logger.
 */
@Suppress("MemberVisibilityCanBePrivate")
class Logger {

    enum class VerboseLevel {
        /** Silences all messages. */
        OFF,
        /** Only errors will be printed into the log. */
        ERROR,
        /** Errors and warnings will be printed into the log. */
        WARNING,
        /** Info logs, errors and warnings will be printed into the log. */
        INFO,
        /** All messages will be printed into the log. */
        DEBUG
    }

    companion object {

        /** Current verbose level. */
        @JvmStatic
        var verboseLevel = VerboseLevel.WARNING

        /** Listener that can tap into the log stream and process it on it's own. */
        var logListener: WMTLogListener? = null

        private val tag = "WMT"

        private fun log(valueFn: () -> String, allowedLevel: VerboseLevel, logFn: (String?, String) -> Unit, listenerFn: ((String) -> Unit)?) {
            val shouldProcess = verboseLevel.ordinal >= allowedLevel.ordinal
            val log = if (shouldProcess || logListener?.followVerboseLevel == false) valueFn() else return
            if (shouldProcess) {
                logFn(tag, log)
            }
            listenerFn?.invoke(log)
        }

        internal fun d(message: String) {
            d { message }
        }

        internal fun d(fn: () -> String) {
            log(fn, VerboseLevel.DEBUG, Log::d, logListener?.let { it::debug })
        }

        internal fun w(message: String) {
            w { message }
        }

        internal fun w(fn: () -> String) {
            log(fn, VerboseLevel.WARNING, Log::w, logListener?.let { it::warning })
        }

        internal fun i(message: String) {
            i { message }
        }

        internal fun i(fn: () -> String) {
            log(fn, VerboseLevel.INFO, Log::i, logListener?.let { it::info })
        }

        internal fun e(message: String) {
            e { message }
        }

        internal fun e(fn: () -> String) {
            log(fn, VerboseLevel.ERROR, Log::e, logListener?.let { it::error })
        }
    }
}
