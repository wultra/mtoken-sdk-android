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

package com.wultra.android.mtokensdk.api.push

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.api.push.model.PushRegistrationRequestObject
import com.wultra.android.powerauth.networking.Api
import com.wultra.android.powerauth.networking.EndpointSignedWithToken
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

internal class PushRegistrationRequest(@SerializedName("requestObject") val requestObject: PushRegistrationRequestObject)

/**
 * API for registering with push server.
 */
internal class PushApi constructor(okHttpClient: OkHttpClient,
                                   baseURL: String,
                                   powerAuthSDK: PowerAuthSDK,
                                   appContext: Context,
                                   tokenProvider: IPowerAuthTokenProvider?) : Api(baseURL, okHttpClient, powerAuthSDK, GsonBuilder(), appContext, tokenProvider) {

    companion object {
        private val endpoint = EndpointSignedWithToken<PushRegistrationRequest, StatusResponse>("api/push/device/register/token", "possession_universal")
    }

    /**
     * Register FCM token with push server.
     */
    fun registerToken(requestObject: PushRegistrationRequest, listener: IApiCallResponseListener<StatusResponse>) {
        post(requestObject, endpoint, null, null, listener)
    }
}