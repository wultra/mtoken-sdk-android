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

/** Utility object for OIDC-related operations, such as PKCE generation and URI handling. */
object OidcUtils {

    /**
     * Creates PKCE (Proof Key for Code Exchange) codes.
     *
     * @param dataLength Length of the random code verifier.
     * @return A [PKCECodes] object containing the `codeVerifier` and `codeChallenge`.
     * @throws IllegalArgumentException If an invalid length is provided.
     * @throws RuntimeException If secure random generation fails.
     */
    fun createPKCE(dataLength: Int): PKCECodes {
        val minLength = 32
        val maxLength = 96
        val length = if (dataLength in minLength..maxLength) dataLength else minLength

        val codeVerifier = getRandomBase64UrlSafe(length)
        val codeChallenge = generateCodeChallenge(codeVerifier)
        return PKCECodes(codeVerifier, codeChallenge)
    }

    /**
     * Generates a random Base64 URL-safe string of the given length.
     *
     * @param dataLength The length of the random byte array.
     * @return A URL-safe Base64-encoded string.
     * @throws RuntimeException If secure random generation fails.
     */
    fun getRandomBase64UrlSafe(dataLength: Int): String {
        return try {
            val secureRandom = SecureRandom()
            val randomBytes = ByteArray(dataLength)
            secureRandom.nextBytes(randomBytes)
            Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        } catch (e: Exception) {
            WMTLogger.e("OIDC: Error generating random Base64 string: ${e.message}")
            throw RuntimeException("Failed to generate a secure random Base64 string", e)
        }
    }

    /**
     * Creates an authorization URI based on the provided OIDC configuration.
     *
     * @param config The [OidcConfig] containing OIDC provider details.
     * @param nonce A unique, randomly generated value to mitigate replay attacks.
     * @param state A unique, randomly generated value to maintain state.
     * @param pkceCodes Optional PKCE codes to include.
     * @return The constructed authorization URI.
     * @throws IllegalArgumentException If the authorization URI is invalid.
     */
    fun createAuthorizationUri(config: OidcConfig, nonce: String, state: String, pkceCodes: PKCECodes?): Uri {
        return try {
            Uri.parse(config.authorizeUri).buildUpon()
                .appendQueryParameter("client_id", config.clientId)
                .appendQueryParameter("redirect_uri", config.redirectUri)
                .appendQueryParameter("scope", config.scopes)
                .appendQueryParameter("state", state)
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("response_type", "code")
                .apply {
                    pkceCodes?.let {
                        appendQueryParameter("code_challenge", it.codeChallenge)
                        appendQueryParameter("code_challenge_method", it.codeMethod)
                    }
                }
                .build()
        } catch (e: Exception) {
            WMTLogger.w("OIDC: Failed to create authorization URI: ${e.message}")
            throw IllegalArgumentException("Failed to create a valid authorization URI", e)
        }
    }

    /**
     * Processes a deeplink URI to extract and validate OIDC activation attributes.
     *
     * @param uriDeeplink The deeplink URI received from the OIDC provider.
     * @param oidcAuthorizationData The expected authorization data (contains state and PKCE verifier).
     * @return The extracted [OidcPowerAuthActivationAttributes].
     * @throws IllegalArgumentException If the URI is invalid or missing required parameters.
     */
    fun processDeeplink(uriDeeplink: Uri, oidcAuthorizationData: OidcAuthorizationData): OidcPowerAuthActivationAttributes {
        val code = uriDeeplink.getQueryParameter("code")
            ?: throw IllegalArgumentException("OIDC: Missing 'code' parameter in deeplink: $uriDeeplink")

        val state = uriDeeplink.getQueryParameter("state")
            ?: throw IllegalArgumentException("OIDC: Missing 'state' parameter in deeplink: $uriDeeplink")

        if (state != oidcAuthorizationData.state) {
            throw IllegalArgumentException("OIDC: Invalid 'state' parameter in deeplink: $uriDeeplink")
        }

        return OidcPowerAuthActivationAttributes(
            providerId = oidcAuthorizationData.providerId,
            code = code,
            nonce = oidcAuthorizationData.nonce,
            codeVerifier = oidcAuthorizationData.codeVerifier
        )
    }

    /** Random generator helpers */

    /**
     * Generates a SHA-256-based code challenge from the given code verifier.
     *
     * @param verifier The code verifier string.
     * @return A SHA-256 hashed Base64 URL-safe string.
     * @throws RuntimeException If hashing fails.
     */
    private fun generateCodeChallenge(verifier: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
            Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate PKCE code challenge", e)
        }
    }
}