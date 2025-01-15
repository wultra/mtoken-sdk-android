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

package com.wultra.android.mtokensdk.oidc

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.oidc.ConfigResponse
import com.wultra.android.mtokensdk.api.oidc.OidcApi
import com.wultra.android.mtokensdk.api.oidc.model.OidcConfigRequest
import com.wultra.android.mtokensdk.api.oidc.model.OidcConfigResponse
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.oidc.models.OidcPowerAuthActivationAttributes
import com.wultra.android.mtokensdk.oidc.models.OidcAuthorizationRequest
import com.wultra.android.mtokensdk.oidc.utils.PKCEUtils
import com.wultra.android.mtokensdk.oidc.utils.RandomGeneratorUtils
import com.wultra.android.mtokensdk.oidc.utils.OidcUriUtils
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.exception.PowerAuthMissingConfigException
import io.getlime.security.powerauth.networking.interfaces.ICancelable
import io.getlime.security.powerauth.networking.response.ICreateActivationListener
import io.getlime.security.powerauth.sdk.PowerAuthActivation
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

class OidcService(
    okHttpClient: OkHttpClient,
    baseURL: String,
    appContext: Context,
    powerAuthSDK: PowerAuthSDK,
    tokenProvider: IPowerAuthTokenProvider? = null,
    userAgent: UserAgent? = null,
    gsonBuilder: GsonBuilder?
) {

    // API class for communication.
    private val oidcApi = OidcApi(okHttpClient, baseURL, appContext, powerAuthSDK, tokenProvider, userAgent, gsonBuilder)

    /**
     * Accept language for the outgoing requests headers.
     * Default value is "en".
     *
     * Standard RFC "Accept-Language" https://tools.ietf.org/html/rfc7231#section-5.3.5
     * Response texts are based on this setting. For example when "de" is set, server
     * will return operation texts in german (if available).
     */
    var acceptLanguage: String
        get() = oidcApi.acceptLanguage
        set(value) {
            oidcApi.acceptLanguage = value
        }

    /**
     * A custom interceptor can intercept each service call.
     *
     * You can use this for request/response logging into your own log system.
     */
    var okHttpInterceptor: OkHttpBuilderInterceptor?
        get() = oidcApi.okHttpInterceptor
        set(value) {
            oidcApi.okHttpInterceptor = value
        }

    /**
     * Prepares the OIDC (OpenID Connect) activation process by performing the following steps:
     *
     * 1. Retrieves the OIDC configuration using the provided `providerId`.
     * 2. Generates PKCE (Proof Key for Code Exchange) codes if PKCE is enabled for the configuration.
     * 3. Creates a `nonce` (a unique value to mitigate replay attacks) and `state` (to maintain state between the request and callback).
     * 4. Constructs an authorization URI, which is used to generate a URL that should be opened in the browser for user authentication.
     *
     * Upon successful execution, the function invokes the provided callback with an [OidcAuthorizationRequest] object containing the authorization URI
     * and other details required for the OIDC flow. If any step fails, the callback is invoked with a [Result.failure].
     *
     * @param providerId A unique identifier for the configuration record, used as a key to retrieve the configuration.
     * @param callback A callback that receives the result of the OIDC preparation. On success, it returns [OidcAuthorizationRequest].
     *                 On failure, it returns an appropriate [Throwable].
     */
    fun prepareOidcActivation(providerId: String, callback: (Result<OidcAuthorizationRequest>) -> Unit) {
        // Get configuration based on providerId
        getConfig(providerId) { configResult ->
            configResult.onSuccess { oidcConfig ->

                // If pkceEnabled create PKCE Codes or continue with null
                val resultPKCE = PKCEUtils.create(oidcConfig.pkceEnabled, 32)
                resultPKCE.onSuccess { pkceCodes ->
                    val nonce = RandomGeneratorUtils.getRandomBase64UrlSafe(32)
                    val state = RandomGeneratorUtils.getRandomBase64UrlSafe(32)

                    // Finally create authorizeUri
                    val authorizeUri = OidcUriUtils.createAuthorizationUri(oidcConfig, nonce, state, pkceCodes)
                    authorizeUri.onSuccess { authUri ->

                        // Callback with success
                        callback(Result.success(OidcAuthorizationRequest(authUri, providerId, nonce, state, pkceCodes?.codeVerifier)))
                    }.onFailure { error ->
                        WMTLogger.e("OIDC: Failed to create authorization Uri: ${error.message}")
                        callback(Result.failure(error))
                    }
                }.onFailure { error ->
                    WMTLogger.e("OIDC: Failed to create PKCE codes: ${error.message}")
                    callback(Result.failure(error))
                }
            }.onFailure { error ->
                WMTLogger.e("OIDC: Failed to get config: ${error.message}")
                callback(Result.failure(error))
            }
        }
    }

    /**
     * Get configuration of OIDC.
     *
     * @param providerId Predefined identification of the configuration record, used as a key for the configuration
     * @param callback Callback with configuration result.
     */
    fun getConfig(
        providerId: String,
        callback: (Result<OidcConfigResponse>) -> Unit
    ) {
        oidcApi.getConfig(
            OidcConfigRequest(providerId),
            object : IApiCallResponseListener<ConfigResponse> {
                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }

                override fun onSuccess(result: ConfigResponse) {
                    callback(Result.success(result.responseObject))
                }
            }
        )
    }

    /**
     * Create a new activation by calling a PowerAuth Standard RESTful API.
     *
     * @receiver PowerAuthSDK
     * @param attributes Data object containing the information required for the activation creation.
     *  - to create attributes see [OidcUriUtils.processDeeplinkOidc]
     * @param listener A callback listener called when the process finishes - it contains an activation fingerprint in case of success or an error in case of failure.
     * @return ICancelable object associated with the running HTTP request.
     * @throws PowerAuthMissingConfigException – thrown in case configuration is not present.
     */
    @Throws(PowerAuthMissingConfigException::class)
    fun PowerAuthSDK.createOidcActivation(attributes: OidcPowerAuthActivationAttributes, listener: ICreateActivationListener): ICancelable? {
        val activation =
            PowerAuthActivation
                .Builder
                .oidcActivation(
                    attributes.providerId,
                    attributes.code,
                    attributes.nonce,
                    attributes.codeVerifier
                )
                .build()
        return createActivation(activation, listener)
    }
}
