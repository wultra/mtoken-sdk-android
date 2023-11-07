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
    data class OperationTOTPData(
        val totp: String,
        @SerializedName("oid")
        val operationId: String
    )

    companion object {

        /** Method accepts deeplink URL and returns payload data or null */
        fun tryLoginDeeplink(uri: Uri?): OperationTOTPData? {
            val query = uri?.query ?: return null
            val queryItems = query.split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }

            if (uri.host == "login" && "code" in queryItems) {
                val code = queryItems["code"]
                return code?.let { parseJWT(it) }
            }

            return null
        }

        /** Method accepts scanned code as a String and returns payload data or null */
        fun getTOTPFromQR(code: String): OperationTOTPData? {
            return parseJWT(code)
        }

        private fun parseJWT(code: String): OperationTOTPData? {
            val jwtParts = code.split(".")
            if (jwtParts.size >= 2) {
                val jwtBase64String = jwtParts[1]
                if (jwtBase64String.isNotEmpty()) {
                    val base64EncodedData = jwtBase64String.toByteArray(Charsets.UTF_8)
                    val dataPayload = Base64.decode(base64EncodedData, Base64.DEFAULT)
                    return try {
                        val json = String(dataPayload, Charsets.UTF_8)
                        Gson().fromJson(json, OperationTOTPData::class.java)
                    } catch (e: Exception) {
                        Logger.e("Failed to decode QR JWT from: ${code}")
                        Logger.e("With error: ${e.message}")
                        null
                    }
                }
            }

            Logger.e("Failed to decode QR JWT from: $jwtParts")
            return null
        }
    }
}
