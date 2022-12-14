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

package com.wultra.android.mtokensdk.push

import android.content.Context
import com.wultra.android.mtokensdk.api.push.PushApi
import com.wultra.android.mtokensdk.api.push.PushRegistrationRequest
import com.wultra.android.mtokensdk.api.push.model.PushRegistrationRequestObject
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Convenience factory method to create an IPushService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context
 * @param baseURL Base URL for push request
 * @param okHttpClient HTTP client instance for networking
 */
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, okHttpClient: OkHttpClient, userAgent: UserAgent? = null): IPushService {
    return PushService(okHttpClient, baseURL, this, appContext, null, userAgent)
}

/**
 * Convenience factory method to create an IPushService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context
 * @param baseURL Base URL for push request
 * @param strategy SSL validation strategy for networking
 */
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy, userAgent: UserAgent? = null): IPushService {
    val builder = OkHttpClient.Builder()
    strategy.configure(builder)
    Logger.configure(builder)
    return createPushService(appContext, baseURL, builder.build(), userAgent)
}

class PushService(okHttpClient: OkHttpClient, baseURL: String, powerAuthSDK: PowerAuthSDK, appContext: Context, tokenProvider: IPowerAuthTokenProvider? = null, userAgent: UserAgent? = null): IPushService {

    override var acceptLanguage: String
        get() = pushApi.acceptLanguage
        set(value) {
            pushApi.acceptLanguage = value
        }

    private val pushApi = PushApi(okHttpClient, baseURL, powerAuthSDK, appContext, tokenProvider, userAgent)

    override fun register(fcmToken: String, callback: (Result<Unit>) -> Unit) {
        pushApi.registerToken(PushRegistrationRequest(PushRegistrationRequestObject(fcmToken)), object :
            IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                callback(Result.success(Unit))
            }

            override fun onFailure(error: ApiError) {
                Logger.e("Failed to register fcm token for WMT push notifications.")
                callback(Result.failure(ApiErrorException(error)))
            }
        })
    }

}