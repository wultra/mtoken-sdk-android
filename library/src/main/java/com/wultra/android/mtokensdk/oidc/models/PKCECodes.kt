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

/**
 * Represents PKCE (Proof Key for Code Exchange) codes used in OAuth 2.0 and OpenID Connect flows
 * to enhance the security of authorization code exchanges.
 *
 * The PKCE mechanism mitigates the risk of authorization code interception attacks by requiring
 * the client to prove possession of a secure random secret (code verifier) during the exchange.
 *
 * @property codeVerifier A securely generated random string used as a proof key.
 * @property codeChallenge A hashed and Base64 URL-safe encoded version of the code verifier,
 *                         used to verify the authorization code during the exchange.
 * @property codeMethod The method used to generate the `codeChallenge`. Defaults to `S256` (SHA-256).
 * @see [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636) for details on the PKCE standard.
 */
data class PKCECodes(
    val codeVerifier: String,
    val codeChallenge: String,
    val codeMethod: String = "S256"
)
