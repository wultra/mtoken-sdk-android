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
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.log.WMTLogger

/**
 * Utility class used for handling Proximity Anti-fraud Checks
 */
class PACUtils {

    /** Data payload which is returned from the parser */
    data class PACData(

        /** The ID of the operation associated with the TOTP */
        @SerializedName("oid")
        val operationId: String,

        /** The actual Time-based one time password */
        @SerializedName(value = "potp", alternate = ["totp"])
        val totp: String?
    )

    companion object {

        /** Method accepts deeplink Uri and returns payload data or null */
        fun parseDeeplink(uri: Uri): PACData? {

            try {
                // Deeplink can have two query items with operationId & optional totp or single query item with JWT value
                uri.getQueryParameter("oid")?.let { operationId ->
                    if (uri.query?.contains(operationId) == false) {
                        WMTLogger.e("Operation could not be resolved - probably contains invalid characters - please, encode the URL first")
                        return null
                    }
                    val totp = uri.getQueryParameter("totp") ?: uri.getQueryParameter("potp")
                    return PACData(operationId, totp)
                } ?: uri.queryParameterNames.firstOrNull()?.let {
                    return parseJWT(uri.getQueryParameter(it) ?: "")
                } ?: run {
                    WMTLogger.e("Failed to parse deeplink. Valid keys not found in Uri: $uri")
                    return null
                }
            } catch (t: Throwable) {
                WMTLogger.e("Failed to parse deeplink - $t")
                return null
            }
        }

        /** Method accepts scanned code as a String and returns PAC data */
        fun parseQRCode(code: String): PACData? {
            val uri = Uri.parse(code)
            // if the QR code is in the deeplink format parse it the same way as the deeplink
            return if (uri.scheme != null) {
                parseDeeplink(uri)
            } else {
                parseJWT(code)
            }
        }

        private fun parseJWT(code: String): PACData? {
            val jwtParts = code.split(".")
            if (jwtParts.size > 1) {
                // At this moment we don't care about header, we want only payload which is the second part of JWT
                val jwtBase64String = jwtParts[1]
                if (jwtBase64String.isNotEmpty()) {
                    val base64EncodedData = jwtBase64String.toByteArray(Charsets.UTF_8)
                    return try {
                        val dataPayload = Base64.decode(base64EncodedData, Base64.DEFAULT)
                        val json = String(dataPayload, Charsets.UTF_8)
                        Gson().fromJson(json, PACData::class.java)
                    } catch (e: Exception) {
                        WMTLogger.e("Failed to decode QR JWT from: $code")
                        WMTLogger.e("With error: ${e.message}")
                        null
                    }
                } else {
                    WMTLogger.e("JWT Payload is empty, jwtParts contain: $jwtParts")
                }
            }

            WMTLogger.e("Failed to decode QR JWT from: $jwtParts")
            return null
        }
    }
}
