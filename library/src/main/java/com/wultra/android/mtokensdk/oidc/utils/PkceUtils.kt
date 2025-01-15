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
import java.security.MessageDigest

/**
 * Represents PKCE (Proof Key for Code Exchange) codes used in OAuth 2.0 and OpenID Connect flows
 * to enhance the security of authorization code exchanges.
 *
 * The PKCE mechanism mitigates the risk of authorization code interception attacks by requiring
 * the client to prove possession of a secure random secret (code verifier) during the exchange.
 *
 * @property codeVerifier A securely generated random string used as a proof key.
 * @property codeChallenge A hashed and Base64 URL-safe encoded version of the code verifier,
 *                         used to verify the authorization code during the exchange.
 * @property codeMethod The method used to generate the `codeChallenge`. Defaults to `S256` (SHA-256).
 * @see [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636) for details on the PKCE standard.
 */
data class PKCECodes(
    val codeVerifier: String,
    val codeChallenge: String,
    val codeMethod: String = "S256"
)

/**
 * Utility class for generating PKCE codes.
 */
object PKCEUtils {

    /**
     * Creates PKCE codes, returning a Result wrapping the codes or an error.
     */
    fun create(enabled: Boolean?, dataLength: Int): Result<PKCECodes?> {
        if (enabled != true) return Result.success(null)

        return try {
            val codeVerifier = RandomGeneratorUtils.getRandomBase64UrlSafe(dataLength)
            val codeChallenge = generateCodeChallenge(codeVerifier)
            Result.success(PKCECodes(codeVerifier, codeChallenge))
        } catch (e: Exception) {
            WMTLogger.e("OIDC PKCE: Error creating PKCE codes: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Generates a SHA-256-based code challenge from the given code verifier.
     */
    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}