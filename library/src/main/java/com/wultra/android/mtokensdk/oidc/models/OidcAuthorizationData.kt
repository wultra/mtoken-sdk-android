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

import android.net.Uri

/**
 * Represents the data required to initiate an OpenID Connect (OIDC) authorization flow.
 *
 * This class encapsulates the parameters needed to:
 * - Open the `authorizeUri` in a web browser to start the OIDC flow.
 * - Ensure the integrity and security of the flow using `nonce` and `state`.
 * - Optionally include PKCE (Proof Key for Code Exchange) support via `codeVerifier`.
 */
data class OidcAuthorizationData(
    /**
     * The URI to be opened in a browser for user authentication and authorization.
     */
    val authorizeUri: Uri,
    /**
     * A unique identifier which was used to getting info for authorizeUri creation
     */
    val providerId: String,
    /**
     * A randomly generated value to prevent replay attacks.
     */
    val nonce: String,
    /**
     * A unique identifier to maintain state between the request and callback.
     */
    val state: String,
    /**
     * An optional PKCE code verifier for enhanced security.
     */
    val codeVerifier: String?
)
