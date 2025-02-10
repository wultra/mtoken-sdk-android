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
 * Represents the attributes required to initiate a PowerAuth activation after completing an OIDC (OpenID Connect) flow.
 *
 * These attributes are extracted from the OIDC flow and are essential for securely starting the PowerAuth activation process.
 * The [OIDCUtils.processDeeplink] method is used to generate an instance of this class by processing
 * the deeplink URI returned by the OIDC authorization flow.
 */
data class OIDCPowerAuthActivationAttributes(

    /** The unique identifier for the OIDC provider configuration. */
    val providerId: String,

    /** The authorization code received from the OIDC flow. */
    val code: String,

    /** The randomly generated value used to ensure the integrity of the OIDC flow. */
    val nonce: String,

    /** The PKCE (Proof Key for Code Exchange) code verifier used during the OIDC flow, if applicable. */
    val codeVerifier: String?
)
