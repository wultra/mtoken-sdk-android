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

import com.wultra.android.mtokensdk.api.oidc.model.OidcConfigResponse
import com.wultra.android.mtokensdk.oidc.models.OidcConfig
import com.wultra.android.mtokensdk.oidc.models.OidcPowerAuthActivationAttributes
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
 *  - to create attributes see [OidcUtils.processDeeplink]
 * @param activationName The activation's name parameter is optional, but recommended to set. You can use the
 * value obtained from {@code Settings.System.getString(getContentResolver(), "device_name")} or let the user
 * set the name.
 * @param listener A callback listener called when the process finishes - it contains an activation fingerprint in case of success or an error in case of failure.
 * @return ICancelable object associated with the running HTTP request.
 * @throws PowerAuthMissingConfigException – thrown in case configuration is not present.
 */
@Throws(PowerAuthMissingConfigException::class)
fun PowerAuthSDK.createOidcActivation(attributes: OidcPowerAuthActivationAttributes, activationName: String? = null, listener: ICreateActivationListener): ICancelable? {
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
 * Maps an `OidcConfigResponse` (network model) to an `OidcConfig` (domain model).
 */
fun OidcConfigResponse.toOidcConfig(): OidcConfig {
    return OidcConfig(
        providerId = this.providerId,
        clientId = this.clientId,
        scopes = this.scopes,
        authorizeUri = this.authorizeUri,
        redirectUri = this.redirectUri,
        pkceEnabled = this.pkceEnabled
    )
}
