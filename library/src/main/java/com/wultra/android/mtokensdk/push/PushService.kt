/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.push

import android.content.Context
import com.wultra.android.mtokensdk.api.IApiCallResponseListener
import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.general.StatusResponse
import com.wultra.android.mtokensdk.api.push.PushApi
import com.wultra.android.mtokensdk.api.push.model.PushRegistrationRequest
import com.wultra.android.mtokensdk.common.IPowerAuthTokenProvider
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.mtokensdk.common.SSLValidationStrategy
import com.wultra.android.mtokensdk.common.TokenManager
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
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, okHttpClient: OkHttpClient): IPushService {
    return PushService(okHttpClient, baseURL, TokenManager(appContext, this.tokenStore))
}

/**
 * Convenience factory method to create an IPushService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context
 * @param baseURL Base URL for push request
 * @param strategy SSL validation strategy for networking
 */
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IPushService {
    val builder = OkHttpClient.Builder()
    strategy.configure(builder)
    Logger.configure(builder)
    return createPushService(appContext, baseURL, builder.build())
}

class PushService(okHttpClient: OkHttpClient, baseURL: String, tokenProvider: IPowerAuthTokenProvider): IPushService {

    override var acceptLanguage: String
        get() = pushApi.acceptLanguage
        set(value) {
            pushApi.acceptLanguage = value
        }

    private val pushApi = PushApi(okHttpClient, baseURL, tokenProvider)

    override fun register(fcmToken: String, listener: IPushRegisterListener) {
        pushApi.registerToken(PushRegistrationRequest(token = fcmToken), object : IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                listener.onSuccess()
            }

            override fun onFailure(e: Throwable) {
                Logger.e("Failed to register fcm token for WMT push notifications.")
                listener.onFailure(ApiError(e))
            }
        })
    }

}