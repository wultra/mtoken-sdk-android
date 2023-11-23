/*
 * Copyright 2023 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.operation

import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.common.Logger

/**
 * Utility class used for handling TOTP
 */
class PACUtils {

    /** Data which is return after parsing PAC code */
    data class PACData(

        /** Time-based one time password used for Proximity antifraud check */
        @SerializedName("oid")
        val operationId: String,

        /** The ID of the operation associated with the PAC */
        val totp: String?
    )

    companion object {

        /** Method accepts deeplink URL and returns PAC data */
        fun parseDeeplink(uri: Uri): PACData? {
            val components = Uri.parse(uri.toString())

            components.getQueryParameter("oid")?.let { operationId ->
                val totp = components.getQueryParameter("totp")
                return PACData(operationId, totp)
            } ?: run {
                Logger.e("Failed to get operationId from query items keys: ${components.queryParameterNames}")
                return null
            }
        }

        /** Method accepts scanned code as a String and returns PAC data */
        fun parseQRCode(code: String): PACData? {
            val encodedURLString = Uri.encode(code)
            val uri = Uri.parse(encodedURLString)

            return parseDeeplink(uri)
        }
    }
}
