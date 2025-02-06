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

package com.wultra.android.mtokensdk.api.oidc

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.oidc.model.OidcConfigRequest
import com.wultra.android.mtokensdk.api.oidc.model.OIDCConfigResponse
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.powerauth.networking.Api
import com.wultra.android.powerauth.networking.E2EEConfiguration
import com.wultra.android.powerauth.networking.EndpointBasic
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.ObjectResponse
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

internal class ConfigResponse(requestObject: OIDCConfigResponse, status: Status): ObjectResponse<OIDCConfigResponse>(requestObject, status)

/**
 * Api for OIDC
 */
internal class OIDCApi(
    okHttpClient: OkHttpClient,
    baseUrl: String,
    appContext: Context,
    powerAuthSDK: PowerAuthSDK,
    tokenProvider: IPowerAuthTokenProvider?,
    userAgent: UserAgent?,
    gsonBuilder: GsonBuilder?
) : Api(baseUrl, okHttpClient, powerAuthSDK, gsonBuilder ?: OperationsUtils.defaultGsonBuilder(), appContext, tokenProvider, userAgent ?: UserAgent.libraryDefault(appContext)) {

    companion object {
        private val getConfig = EndpointBasic<OidcConfigRequest, ConfigResponse>("/api/config/oidc", E2EEConfiguration.APPLICATION_SCOPE)
    }

    var okHttpInterceptor: OkHttpBuilderInterceptor? = null

    /**
     * Get OIDC config.
     */
    fun getConfig(request: OidcConfigRequest, listener: IApiCallResponseListener<ConfigResponse>) {
        post(request, getConfig, null, okHttpInterceptor, listener)
    }
}
