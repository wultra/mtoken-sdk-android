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
import com.wultra.android.mtokensdk.api.push.PushApi
import com.wultra.android.mtokensdk.api.push.PushRegistrationRequest
import com.wultra.android.mtokensdk.api.push.model.PushRegistrationRequestObject
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
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
fun PowerAuthSDK.createPushService(appContext: Context, baseURL: String, okHttpClient: OkHttpClient): IPushService {
    return PushService(okHttpClient, baseURL, this, appContext)
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

class PushService(okHttpClient: OkHttpClient, baseURL: String, powerAuthSDK: PowerAuthSDK, appContext: Context, tokenProvider: IPowerAuthTokenProvider? = null): IPushService {

    override var acceptLanguage: String
        get() = pushApi.acceptLanguage
        set(value) {
            pushApi.acceptLanguage = value
        }

    private val pushApi = PushApi(okHttpClient, baseURL, powerAuthSDK, appContext, tokenProvider)

    override fun register(fcmToken: String, listener: IPushRegisterListener) {
        pushApi.registerToken(PushRegistrationRequest(PushRegistrationRequestObject(fcmToken)), object :
            IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                listener.onSuccess()
            }

            override fun onFailure(error: ApiError) {
                Logger.e("Failed to register fcm token for WMT push notifications.")
                listener.onFailure(error)
            }
        })
    }

}