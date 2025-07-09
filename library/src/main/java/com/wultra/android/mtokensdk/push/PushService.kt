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
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Service, that communicates with Mobile Token API that handles registration for
 * push notifications.
 */
class PushService(powerAuthSDK: PowerAuthSDK, appContext: Context, okHttpClient: OkHttpClient, baseURL: String, tokenProvider: IPowerAuthTokenProvider? = null, userAgent: UserAgent? = null) {
    /**
     * Accept language for the outgoing requests headers.
     * Default value is "en".
     * Changing this value updates the accept language of the underlying pushApi.
     *
     * Standard RFC "Accept-Language" https://tools.ietf.org/html/rfc7231#section-5.3.5
     * Response texts are based on this setting. For example when "de" is set, server
     * will return operation texts in german (if available).
     */
    var acceptLanguage: String
        get() = pushApi.acceptLanguage
        set(value) {
            pushApi.acceptLanguage = value
        }

    /**
     * A custom interceptor can intercept each service call.
     *
     * You can use this for request/response logging into your own log system.
     */
    var okHttpInterceptor: OkHttpBuilderInterceptor?
        get() = pushApi.okHttpInterceptor
        set(value) {
            pushApi.okHttpInterceptor = value
        }

    private val pushApi = PushApi(okHttpClient, baseURL, powerAuthSDK, appContext, tokenProvider, userAgent)

    /**
     * Registers FCM on PowerAuth backend to receive notifications about operations and inbox.
     *
     * @param data Token and push platform (Firebase Cloud Messaging or Huawei Push)
     * @param callback Result listener
     */
    fun register(data: PushData, callback: (result: Result<Unit>) -> Unit) {
        val platform = when (data.platform) {
            PushPlatform.FCM -> PushRegistrationRequestObject.Platform.FCM
            PushPlatform.HMS -> PushRegistrationRequestObject.Platform.HMS
        }

        register(data.token, platform, callback)
    }

    private fun register(token: String, platform: PushRegistrationRequestObject.Platform, callback: (Result<Unit>) -> Unit) {
        pushApi.registerToken(
            PushRegistrationRequest(PushRegistrationRequestObject(token, platform)),
            object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    callback(Result.success(Unit))
                }

                override fun onFailure(error: ApiError) {
                    WMTLogger.e("Failed to register fcm token for WMT push notifications.")
                    callback(Result.failure(ApiErrorException(error)))
                }
            }
        )
    }

    // deprecated API
    @Deprecated("Use register function with `data` parameter as a replacement")
    fun register(fcmToken: String, callback: (Result<Unit>) -> Unit) {
        register(fcmToken, PushRegistrationRequestObject.Platform.ANDROID, callback)
    }

    @Deprecated("Use register function with `data` parameter as a replacement")
    fun registerHuawei(hmsToken: String, callback: (result: Result<Unit>) -> Unit) {
        register(hmsToken, PushRegistrationRequestObject.Platform.HUAWEI, callback)
    }
}
