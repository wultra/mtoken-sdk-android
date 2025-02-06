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
package com.wultra.android.mtokensdk.oidc.models

data class OIDCConfig(
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
