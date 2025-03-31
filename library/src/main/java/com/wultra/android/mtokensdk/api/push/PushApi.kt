/*
 * Copyright 2022 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.api.push

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.push.model.PushRegistrationRequestObject
import com.wultra.android.powerauth.networking.Api
import com.wultra.android.powerauth.networking.EndpointSignedWithToken
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

internal class PushRegistrationRequest(requestObject: PushRegistrationRequestObject): ObjectRequest<PushRegistrationRequestObject>(requestObject)

/**
 * API for registering with push server.
 */
internal class PushApi constructor(
    okHttpClient: OkHttpClient,
    baseURL: String,
    powerAuthSDK: PowerAuthSDK,
    appContext: Context,
    tokenProvider: IPowerAuthTokenProvider?,
    userAgent: UserAgent?
) : Api(baseURL, okHttpClient, powerAuthSDK, GsonBuilder(), appContext, tokenProvider, userAgent ?: UserAgent.libraryDefault(appContext)) {

    companion object {
        private val endpoint = EndpointSignedWithToken<PushRegistrationRequest, StatusResponse>("api/push/device/register/token", "possession_universal")
    }

    var okHttpInterceptor: OkHttpBuilderInterceptor? = null

    /**
     * Register FCM token with push server.
     */
    fun registerToken(requestObject: PushRegistrationRequest, listener: IApiCallResponseListener<StatusResponse>) {
        post(requestObject, endpoint, null, okHttpInterceptor, listener)
    }
}
