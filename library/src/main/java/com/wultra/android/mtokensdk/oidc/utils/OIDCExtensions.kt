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

import com.wultra.android.mtokensdk.api.oidc.model.OIDCConfigResponse
import com.wultra.android.mtokensdk.oidc.models.OIDCConfig
import com.wultra.android.mtokensdk.oidc.models.OIDCPowerAuthActivationAttributes
import io.getlime.security.powerauth.exception.PowerAuthMissingConfigException
import io.getlime.security.powerauth.networking.interfaces.ICancelable
import io.getlime.security.powerauth.networking.response.ICreateActivationListener
import io.getlime.security.powerauth.sdk.PowerAuthActivation
import io.getlime.security.powerauth.sdk.PowerAuthSDK

/**
 * Create a new activation by calling a PowerAuth Standard RESTful API.
 *
 * @receiver PowerAuthSDK
 * @param attributes Data object containing the information required for the activation creation.
 *  - to create attributes see [OIDCUtils.processDeeplink]
 * @param activationName The activation's name parameter is optional but recommended to set. You can use the
 * value obtained from {@code Settings.System.getString(getContentResolver(), "device_name")} or let the user
 * set the name.
 * @param listener A callback listener called when the process finishes - it contains an activation fingerprint in case of success or an error in case of failure.
 * @return ICancelable object associated with the running HTTP request.
 */
fun PowerAuthSDK.createOIDCActivation(attributes: OIDCPowerAuthActivationAttributes, activationName: String? = null, listener: ICreateActivationListener): ICancelable? {
    val activationBuilder =
        PowerAuthActivation
            .Builder
            .oidcActivation(
                attributes.providerId,
                attributes.code,
                attributes.nonce,
                attributes.codeVerifier
            )
    activationName?.let { activationBuilder.setActivationName(it) }
    val activation = activationBuilder.build()
    return createActivation(activation, listener)
}

/**
 * Maps an `OIDCConfigResponse` (network model) to an `OIDCConfig` (domain model).
 */
internal fun OIDCConfigResponse.toOidcConfig(): OIDCConfig {
    return OIDCConfig(
        providerId = this.providerId,
        clientId = this.clientId,
        scopes = this.scopes,
        authorizeUri = this.authorizeUri,
        redirectUri = this.redirectUri,
        pkceEnabled = this.pkceEnabled
    )
}
