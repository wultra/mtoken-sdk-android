package com.wultra.android.mtokensdk.api.oidc.model

import com.google.gson.annotations.SerializedName

/**
 * Config Response data contains essential OIDC configuration values for authentication.
 */
internal data class OIDCConfigResponse(

    /** Provider's identifier. */
    @SerializedName("providerId")
    val providerId: String,

    /** Identification of the OAuth 2.0 client, to form the URL for authorize request */
    @SerializedName("clientId")
    val clientId: String,

    /** OAuth 2.0 scopes, to form the URL for authorize request */
    @SerializedName("scopes")
    val scopes: String,

    /** OAuth 2.0 authorize URI, to form the URL for authorize request */
    @SerializedName("authorizeUri")
    val authorizeUri: String,

    /** OAuth 2.0 redirect URI, the endpoint to which the OAuth 2.0 server can send responses. */
    @SerializedName("redirectUri")
    val redirectUri: String,

    /** If PKCE(Proof Key for Code Exchange) extension should be used */
    @SerializedName("pkceEnabled")
    val pkceEnabled: Boolean
)
