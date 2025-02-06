/*
 * Copyright 2025 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.oidc.utils

import android.net.Uri
import android.util.Base64
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.oidc.models.OIDCAuthorizationRequest
import com.wultra.android.mtokensdk.oidc.models.OIDCConfig
import com.wultra.android.mtokensdk.oidc.models.OIDCPowerAuthActivationAttributes
import com.wultra.android.mtokensdk.oidc.models.PKCECodes
import java.security.MessageDigest
import java.security.SecureRandom

/** Utility object for OIDC-related operations, such as PKCE generation and URI handling. */
object OIDCUtils {

    private const val MIN_LENGTH = 32 // min is 32-octet sequence == Base64 43 URL safe characters
    private const val MAX_LENGTH = 96 // max is 96-octet sequence == Base64 128 URL safe characters

    /**
     * Creates PKCE (Proof Key for Code Exchange) codes.
     *
     * @param dataLength The number of raw bytes to generate for the code verifier before Base64 encoding.
     *                     If the provided length is outside the allowed range, the minimum length is used.
     * @return A [PKCECodes] object containing the `codeVerifier`, `codeChallenge` and used hash method.
     * @throws IllegalArgumentException If an invalid length is provided.
     * @throws RuntimeException If secure random generation fails.
     * @see [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636) for details on the PKCE standard.
     */
    fun createPKCE(dataLength: Int): PKCECodes {
        val length = if (dataLength in MIN_LENGTH..MAX_LENGTH) dataLength else MIN_LENGTH

        val codeVerifier = getRandomBase64UrlSafe(length)

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        val codeChallenge = Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        return PKCECodes(codeVerifier, codeChallenge)
    }

    /**
     * Generates a random Base64 URL-safe string of the given length.
     *
     * @param dataLength The number of raw bytes to generate before Base64 encoding.
     * @return A URL-safe Base64-encoded string without padding.
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
     * This URI is then used to initiate the authentication login flow.
     *
     * @param config The [OIDCConfig] containing OIDC provider details.
     * @param nonce A unique, randomly generated value to mitigate replay attacks.
     * @param state A unique, randomly generated value to maintain state.
     * @param pkceCodes Optional PKCE codes for additional security.
     * @return The constructed authorization URI with query parameters.
     * @throws IllegalArgumentException If the authorization URI is invalid.
     */
    fun createAuthorizationUri(config: OIDCConfig, nonce: String, state: String, pkceCodes: PKCECodes?): Uri {
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
     * @param oidcAuthorizationRequest The expected authorization data (contains state and PKCE verifier).
     * @return The extracted [OIDCPowerAuthActivationAttributes].
     * @throws IllegalArgumentException If the URI is invalid or missing required parameters.
     */
    fun processDeeplink(uriDeeplink: Uri, oidcAuthorizationRequest: OIDCAuthorizationRequest): OIDCPowerAuthActivationAttributes {
        val code = uriDeeplink.getQueryParameter("code")
            ?: throw IllegalArgumentException("OIDC: Missing 'code' parameter in deeplink: $uriDeeplink")

        val state = uriDeeplink.getQueryParameter("state")
            ?: throw IllegalArgumentException("OIDC: Missing 'state' parameter in deeplink: $uriDeeplink")

        if (state != oidcAuthorizationRequest.state) {
            throw IllegalArgumentException("OIDC: Invalid 'state' parameter in deeplink: $uriDeeplink")
        }

        return OIDCPowerAuthActivationAttributes(
            providerId = oidcAuthorizationRequest.providerId,
            code = code,
            nonce = oidcAuthorizationRequest.nonce,
            codeVerifier = oidcAuthorizationRequest.codeVerifier
        )
    }
}
