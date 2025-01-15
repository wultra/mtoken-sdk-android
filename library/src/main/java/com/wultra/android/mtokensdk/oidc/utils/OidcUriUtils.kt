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
import com.wultra.android.mtokensdk.api.oidc.model.OidcConfigResponse
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.oidc.models.OidcPowerAuthActivationAttributes
import com.wultra.android.mtokensdk.oidc.models.OidcAuthorizationRequest

/**
 * Utility class for generating and processing URIs in the context of OIDC (OpenID Connect) flows.
 *
 * This utility provides methods to:
 * - Create an authorization URI for initiating the OIDC flow.
 * - Process a deeplink URI to extract and validate OIDC activation attributes.
 */
object OidcUriUtils {

    /**
     * Creates an authorization URI based on the provided OIDC configuration, nonce, state, and PKCE codes.
     *
     * The authorization URI is used to redirect the user to the authentication provider's web interface
     * for initiating the OIDC authorization flow.
     *
     * @param config The [OidcConfigResponse] containing information returned from backend to getConfig call.
     * @param nonce A unique, randomly generated value to mitigate replay attacks.
     * @param state A unique, randomly generated value to maintain state between the request and callback.
     * @param pkceCodes The PKCE (Proof Key for Code Exchange) codes to include in the authorization request,
     *                  if applicable. Can be `null` if PKCE is not used.
     *
     * @return A [Result] containing:
     * - [Uri]: The successfully constructed authorization URI.
     * - [Throwable]: An error if the URI creation process fails (e.g., invalid input or malformed URI).
     */
    fun createAuthorizationUri(config: OidcConfigResponse, nonce: String, state: String, pkceCodes: PKCECodes?): Result<Uri> {
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
     * Processes a deeplink URI to validate its state and extract OIDC activation attributes.
     *
     * @param oidcAuthorizationRequest The [OIDCAuth] object containing the expected state and other necessary data
     *                 for the OIDC flow.
     * @param uriDeeplink The deeplink URI received from the OIDC provider during the authorization process.
     *
     * @return An [OIDCActivationAttributes] object containing the extracted activation attributes, or `null`
     *         if the validation fails (e.g., missing or invalid parameters).
     *
     * ### Validation Logic:
     * - Ensures the `code` parameter is present in the URI.
     * - Ensures the `state` parameter is present and matches the expected value in [OIDCAuth].
     */
    fun processDeeplinkOidc(oidcAuthorizationRequest: OidcAuthorizationRequest, uriDeeplink: Uri): OidcPowerAuthActivationAttributes? {
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

        if (state != oidcAuthorizationRequest.state) {
            WMTLogger.e("OIDC: Invalid 'state' in URL: $uriDeeplink")
            return null
        }

        return OidcPowerAuthActivationAttributes(
            oidcAuthorizationRequest.providerId,
            code,
            oidcAuthorizationRequest.nonce,
            oidcAuthorizationRequest.codeVerifier
        )
    }
}