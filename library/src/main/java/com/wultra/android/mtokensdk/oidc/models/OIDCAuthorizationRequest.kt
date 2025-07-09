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

import android.net.Uri

/**
 * Represents the data required to initiate an OpenID Connect (OIDC) authorization flow.
 *
 * This class encapsulates the parameters needed to:
 * - Open the `authorizeUri` in a web browser to start the OIDC flow.
 * - Ensure the integrity and security of the flow using `nonce` and `state`.
 * - Optionally include PKCE (Proof Key for Code Exchange) support via `codeVerifier`.
 */
data class OIDCAuthorizationRequest(

    /** The URI to be opened in a browser for user authentication and authorization. */
    val authorizeUri: Uri,

    /** A unique identifier which was used to getting info for authorizeUri creation */
    val providerId: String,

    /** A randomly generated value to prevent replay attacks. */
    val nonce: String,

    /** A unique identifier to maintain state between the request and callback. */
    val state: String,

    /** An optional PKCE code verifier for enhanced security. */
    val codeVerifier: String?
)
