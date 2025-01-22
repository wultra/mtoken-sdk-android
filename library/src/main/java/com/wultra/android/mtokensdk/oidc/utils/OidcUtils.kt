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

import android.net.Uri
import android.util.Base64
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.oidc.models.OidcAuthorizationData
import com.wultra.android.mtokensdk.oidc.models.OidcConfig
import com.wultra.android.mtokensdk.oidc.models.OidcPowerAuthActivationAttributes
import com.wultra.android.mtokensdk.oidc.models.PKCECodes
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.ceil

object OidcUtils {

    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"

    /**
     * Creates PKCE codes, returning a Result wrapping the codes.
     * */
    fun createPKCE(dataLength: Int): Result<PKCECodes> {
        return try {
            val codeVerifier = getRandomBase64UrlSafe(dataLength)
            val codeChallenge = generateCodeChallenge(codeVerifier)
            Result.success(PKCECodes(codeVerifier, codeChallenge))
        } catch (e: Exception) {
            WMTLogger.e("OIDC PKCE: Error creating PKCE codes: ${e.message}")
            Result.failure(e)
        }
    }

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
     * Creates an authorization URI based on the provided OIDC configuration, nonce, state, and PKCE codes.
     *
     * The authorization URI is used to redirect the user to the authentication provider's web interface
     * for initiating the OIDC authorization flow.
     *
     * @param config The [OidcConfig] containing information returned from backend to getConfig call.
     * @param nonce A unique, randomly generated value to mitigate replay attacks.
     * @param state A unique, randomly generated value to maintain state between the request and callback.
     * @param pkceCodes The PKCE (Proof Key for Code Exchange) codes to include in the authorization request,
     *                  if applicable. Can be `null` if PKCE is not used.
     *
     * @return A [Result] containing:
     * - [Uri]: The successfully constructed authorization URI.
     * - [Throwable]: An error if the URI creation process fails (e.g., invalid input or malformed URI).
     */
    fun createAuthorizationUri(config: OidcConfig, nonce: String, state: String, pkceCodes: PKCECodes?): Result<Uri> {
        return runCatching {
            Uri.parse(config.authorizeUri).buildUpon()
                .appendQueryParameter("client_id", config.clientId)
                .appendQueryParameter("redirect_uri", config.redirectUri)
                .appendQueryParameter("scope", config.scopes)
                .appendQueryParameter("state", state)
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("response_type", "code")
                .apply {
                    pkceCodes?.let { codes ->
                        appendQueryParameter("code_challenge", codes.codeChallenge)
                        appendQueryParameter("code_challenge_method", codes.codeMethod)
                    }
                }
                .build()
        }.onFailure { e ->
            WMTLogger.w("OIDC: Failed to create authorization URI: ${e.message}")
        }
    }

    /**
     *
     * Uri Utils
     * Processes a deeplink URI to validate its state and extract OIDC activation attributes.
     *
     * @param oidcAuthorizationData The [OidcAuthorizationData] object containing the expected state and other necessary data
     *                 for the OIDC flow.
     * @param uriDeeplink The deeplink URI received from the OIDC provider during the authorization process.
     *
     * @return An [OIDCActivationAttributes] object containing the extracted activation attributes, or `null`
     *         if the validation fails (e.g., missing or invalid parameters).
     *
     * ### Validation Logic:
     * - Ensures the `code` parameter is present in the URI.
     * - Ensures the `state` parameter is present and matches the expected value in [OidcAuthorizationData].
     */
    fun processDeeplink(oidcAuthorizationData: OidcAuthorizationData, uriDeeplink: Uri): OidcPowerAuthActivationAttributes? {
        val code = uriDeeplink.getQueryParameter("code")
        if (code == null) {
            WMTLogger.e("OIDC: Deeplink didn't contain a 'code' in URL: $uriDeeplink")
            return null
        }

        val state = uriDeeplink.getQueryParameter("state")
        if (state == null) {
            WMTLogger.e("OIDC: Missing 'state' in URL: $uriDeeplink")
            return null
        }

        if (state != oidcAuthorizationData.state) {
            WMTLogger.e("OIDC: Invalid 'state' in URL: $uriDeeplink")
            return null
        }

        return OidcPowerAuthActivationAttributes(
            oidcAuthorizationData.providerId,
            code,
            oidcAuthorizationData.nonce,
            oidcAuthorizationData.codeVerifier
        )
    }

    /** Random generator helpers */
    // Generates a fallback random Base64 string when secure random generation fails.
    private fun generateFallbackBase64(length: Int): String {
        val base64Length = ceil(length * 8 / 6.0).toInt()
        return Base64.encodeToString(generateRandomString(base64Length).toByteArray(), Base64.DEFAULT)
    }

    // Generates a random alphanumeric string of the specified length.
    private fun generateRandomString(length: Int): String {
        return (1..length).map { CHARSET.random() }.joinToString("")
    }

    /** PKCE helper */
    // Generates a SHA-256-based code challenge from the given code verifier.
    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
