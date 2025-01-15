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

package com.wultra.android.mtokensdk.oidc.models

/**
 * Represents the attributes required to initiate a PowerAuth activation after completing an OIDC (OpenID Connect) flow.
 *
 * These attributes are extracted from the OIDC flow and are essential for securely starting the PowerAuth activation process.
 * The [UriUtils.processDeeplinkOidc] method is used to generate an instance of this class by processing
 * the deeplink URI returned by the OIDC authorization flow.
 */
data class OidcPowerAuthActivationAttributes(
    /**
     * The unique identifier for the OIDC provider configuration.
     */
    val providerId: String,
    /**
     * The authorization code received from the OIDC flow.
     */
    val code: String,
    /**
     * The randomly generated value used to ensure the integrity of the OIDC flow.
     */
    val nonce: String,
    /**
     * The PKCE (Proof Key for Code Exchange) code verifier used during the OIDC flow, if applicable.
     */
    val codeVerifier: String?
)