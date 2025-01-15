/*
 * Copyright (c) 2025, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.oidc.utils

import android.util.Base64
import com.wultra.android.mtokensdk.log.WMTLogger
import java.security.SecureRandom
import kotlin.math.ceil

object RandomGeneratorUtils {

    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"

    /**
     * Generates a random Base64 URL-safe string of the given length.
     */
    fun getRandomBase64UrlSafe(dataLength: Int): String {
        return try {
            val secureRandom = SecureRandom()
            val randomBytes = ByteArray(dataLength)
            secureRandom.nextBytes(randomBytes)
            Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        } catch (e: Exception) {
            WMTLogger.e("OIDC: Error generating random Base64 string: ${e.message}")
            WMTLogger.i("OIDC: generating random fallback")
            generateFallbackBase64(dataLength)
        }
    }

    /**
     * Generates a fallback random Base64 string when secure random generation fails.
     */
    private fun generateFallbackBase64(length: Int): String {
        val base64Length = ceil(length * 8 / 6.0).toInt()
        return Base64.encodeToString(generateRandomString(base64Length).toByteArray(), Base64.DEFAULT)
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     */
    private fun generateRandomString(length: Int): String {
        return (1..length).map { CHARSET.random() }.joinToString("")
    }
}