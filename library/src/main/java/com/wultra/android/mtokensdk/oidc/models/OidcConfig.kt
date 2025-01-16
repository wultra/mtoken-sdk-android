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

data class OidcConfig(
    /**
     * Provider's identifier.
     */
    val providerId: String,

    /**
     * Identification of the OAuth 2.0 client, to form the URL for authorize request
     */
    val clientId: String,

    /**
     * OAuth 2.0 scopes, to form the URL for authorize request
     */
    val scopes: String,

    /**
     * OAuth 2.0 authorize URI, to form the URL for authorize request
     */
    val authorizeUri: String,

    /**
     *  OAuth 2.0 redirect URI, the endpoint to which the OAuth 2.0 server can send responses.
     */
    val redirectUri: String,

    /**
     * If PKCE(Proof Key for Code Exchange) extension should be used
     */
    val pkceEnabled: Boolean
)
