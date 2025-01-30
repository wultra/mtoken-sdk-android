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
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.oidc.models.OidcAuthorizationData
import com.wultra.android.mtokensdk.oidc.models.OidcConfig
import com.wultra.android.mtokensdk.oidc.models.PKCECodes
import com.wultra.android.mtokensdk.oidc.utils.OidcUtils
import com.wultra.android.mtokensdk.oidc.utils.toOidcConfig
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

class OidcService(
    powerAuthSDK: PowerAuthSDK,
    appContext: Context,
    okHttpClient: OkHttpClient,
    baseURL: String,
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
     * Get configuration of OIDC.
     *
     * @param providerId Predefined identification of the configuration record, used as a key for the configuration
     * @param callback Callback with configuration result.
     */
    fun getConfig(
        providerId: String,
        callback: (Result<OidcConfig>) -> Unit
    ) {
        oidcApi.getConfig(
            OidcConfigRequest(providerId),
            object : IApiCallResponseListener<ConfigResponse> {
                override fun onFailure(error: ApiError) {
                    WMTLogger.e("OIDC: Failed to get config: ${error.e.message}")
                    callback(Result.failure(ApiErrorException(error)))
                }

                override fun onSuccess(result: ConfigResponse) {
                    callback(Result.success(result.responseObject.toOidcConfig()))
                }
            }
        )
    }

    /**
     * Prepares the OIDC (OpenID Connect) authorization data required for the activation process.
     *
     * The function performs the following steps:
     * 1. Generates PKCE (Proof Key for Code Exchange) codes if PKCE is enabled in the provided configuration.
     * 2. Creates a `nonce` (a unique value to mitigate replay attacks) and a `state` (to maintain state between the request and callback).
     * 3. Creates data containing an authorization URI that shall be used to open a browser for user authentication.
     *
     * @param oidcConfig The OIDC configuration, either retrieved dynamically (e.g., using `getConfig`) or defined statically.
     * @param callback A callback that receives the result of the authorization data preparation.
     *                 - On success: Returns [OidcAuthorizationData]
     *                 - On failure: Returns an appropriate [Throwable].
     */
    fun prepareOidcAuthorizationData(oidcConfig: OidcConfig, callback: (Result<OidcAuthorizationData>) -> Unit) {
        try {
            val pkceCodes = createPKCE(oidcConfig.pkceEnabled, 32)
            val nonce = OidcUtils.getRandomBase64UrlSafe(32)
            val state = OidcUtils.getRandomBase64UrlSafe(32)

            val authorizeUri = OidcUtils.createAuthorizationUri(oidcConfig, nonce, state, pkceCodes)

            callback(Result.success(OidcAuthorizationData(authorizeUri, oidcConfig.providerId, nonce, state, pkceCodes?.codeVerifier)))
        } catch (e: Exception) {
            WMTLogger.e("OIDC: Failed to prepare OIDC authorization data: ${e.message}")
            callback(Result.failure(e))
        }
    }

    /**
     * Creates PKCE codes if enabled.
     *
     * @param enabled Whether PKCE is enabled.
     * @param dataLength The length of the PKCE verifier.
     * @return A [PKCECodes] object if PKCE is enabled, or `null` otherwise.
     * @throws Exception If PKCE generation fails.
     */
    private fun createPKCE(enabled: Boolean, dataLength: Int): PKCECodes? {
        return if (!enabled) null else OidcUtils.createPKCE(dataLength)
    }
}
