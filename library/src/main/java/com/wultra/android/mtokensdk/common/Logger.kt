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
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import java.lang.StringBuilder

/* ktlint-disable indent */

/**
 * Logger provides simple logging facility.
 *
 * Logs are written with "WMT" tag to standard [android.util.Log] logger.
 */
class Logger {

    enum class VerboseLevel {
        /** Silences all messages. */
        OFF,
        /** Only errors will be printed into the log. */
        ERROR,
        /** Errors and warnings will be printed into the log. */
        WARNING,
        /** All messages will be printed into the log. */
        DEBUG
    }

    companion object {

        /** Current verbose level. */
        @JvmStatic
        var verboseLevel = VerboseLevel.WARNING

        private val tag = "WMT"

        internal fun d(message: String) {
            if (verboseLevel.ordinal >= VerboseLevel.DEBUG.ordinal) {
                Log.d(tag, message)
            }
        }

        internal fun d(fn: () -> String) {
            if (verboseLevel.ordinal >= VerboseLevel.DEBUG.ordinal) {
                Log.d(tag, fn())
            }
        }

        internal fun w(message: String) {
            if (verboseLevel.ordinal >= VerboseLevel.WARNING.ordinal) {
                Log.w(tag, message)
            }
        }

        internal fun w(fn: () -> String) {
            if (verboseLevel.ordinal >= VerboseLevel.WARNING.ordinal) {
                Log.w(tag, fn())
            }
        }

        internal fun e(message: String, t: Throwable? = null) {
            if (verboseLevel.ordinal >= VerboseLevel.ERROR.ordinal) {
                Log.e(tag, message, t)
            }
        }

        internal fun e(fn: () -> String) {
            if (verboseLevel.ordinal >= VerboseLevel.ERROR.ordinal) {
                Log.e(tag, fn())
            }
        }

        internal fun configure(builder: OkHttpClient.Builder) {
            builder.addInterceptor { chain ->
                val request = chain.request()
                d {
                    var body = ""
                    try {
                        val buffer = Buffer()
                        request.newBuilder().build().body()?.writeTo(buffer)
                        body = buffer.readUtf8()
                    } catch (e: Throwable) {
                        e("Failed to parse request body")
                    }

                    "\n--- WMT REQUEST ---" +
                    "\n- URL: ${request.method()} - ${request.url()}" +
                    "\n- Headers: ${request.headers().forLog()}" +
                    "\n- Body: $body"
                }

                val response: Response

                try {
                    response = chain.proceed(request)
                } catch (e: Throwable) {
                    d {
                        "\n--- WMT REQUEST FAILED ---" +
                        "\n- URL: ${request.method()} - ${request.url()}" +
                        "\n- Error: $e"
                    }
                    throw e
                }

                d {
                    "\n--- WMT RESPONSE ---" +
                    "\n- URL: ${response.request().method()} - ${response.request().url()}" +
                    "\n- Status code: ${response.code()}" +
                    "\n- Headers: ${response.headers().forLog()}" +
                    "\n- Body: ${response.peekBody(10_000).string()}" // allow max 10 KB of text
                }

                response
            }
        }
    }
}

private fun Headers.forLog(): String {
    val result = StringBuilder()
    for (i in 0 until size()) {
        result.append("\n  - ").append(name(i)).append(": ").append(value(i))
    }
    return result.toString()
}
