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
import com.wultra.android.mtokensdk.common.Logger

/**
 * Utility class used for handling TOTP
 */
class TOTPUtils {

    /** Data payload which is returned from JWT parser */
    data class OperationTOTPData(

        /** The ID of the operations associated with the TOTP */
        val totp: String,

        /** The actual Time-based one time password */
        @SerializedName("oid")
        val operationId: String
    )

    companion object {

        /** Method accepts deeplink Uri and returns payload data or null */
        fun parseDeeplink(uri: Uri?): OperationTOTPData? {
            val query = uri?.query ?: return null
            val queryItems = query.split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }

            queryItems["code"]?.let {
                return parseJWT(it)
            } ?: run {
                Logger.e("Failed to parse deeplink. Key `code` not found")
            }

            Logger.e("Failed to parse deeplink from $uri")
            return null
        }

        /** Method accepts scanned code as a String and returns payload data or null */
        fun parseQRCode(code: String): OperationTOTPData? {
            return parseJWT(code)
        }

        private fun parseJWT(code: String): OperationTOTPData? {
            val jwtParts = code.split(".")
            if (jwtParts.size > 1) {
                // At this moment we don't care about header, we want only payload which is the second part of JWT
                val jwtBase64String = jwtParts[1]
                if (jwtBase64String.isNotEmpty()) {
                    val base64EncodedData = jwtBase64String.toByteArray(Charsets.UTF_8)
                    return try {
                        val dataPayload = Base64.decode(base64EncodedData, Base64.DEFAULT)
                        val json = String(dataPayload, Charsets.UTF_8)
                        Gson().fromJson(json, OperationTOTPData::class.java)
                    } catch (e: Exception) {
                        Logger.e("Failed to decode QR JWT from: $code")
                        Logger.e("With error: ${e.message}")
                        null
                    }
                } else {
                    Logger.e("JWT Payload is empty, jwtParts contain: $jwtParts")
                }
            }

            Logger.e("Failed to decode QR JWT from: $jwtParts")
            return null
        }
    }
}
