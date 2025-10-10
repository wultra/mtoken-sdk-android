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

package com.wultra.android.mtokensdk.api.oidc

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.oidc.model.OIDCConfigRequest
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
        private val getConfig = EndpointBasic<OIDCConfigRequest, ConfigResponse>("/api/config/oidc", E2EEConfiguration.APPLICATION_SCOPE)
    }

    var okHttpInterceptor: OkHttpBuilderInterceptor? = null

    /** Get OIDC config. */
    fun getConfig(request: OIDCConfigRequest, listener: IApiCallResponseListener<ConfigResponse>) {
        post(request, getConfig, null, okHttpInterceptor, listener)
    }
}
