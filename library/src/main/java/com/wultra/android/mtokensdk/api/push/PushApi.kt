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

import com.wultra.android.mtokensdk.api.Api
import com.wultra.android.mtokensdk.api.GsonRequestBodyBytes
import com.wultra.android.mtokensdk.api.general.StatusResponse
import com.wultra.android.mtokensdk.api.push.model.PushRegistrationRequest
import com.wultra.android.mtokensdk.common.IPowerAuthTokenProvider
import com.wultra.android.mtokensdk.common.TokenManager
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

/**
 * API for registering with push server.
 */
internal class PushApi constructor(okHttpClient: OkHttpClient,
                                   baseURL: String,
                                   private val tokenManager: IPowerAuthTokenProvider) : Api(okHttpClient,baseURL) {

    private val PUSH_URL = constructApiUrl("api/push/device/register/token")

    /**
     * Register FCM token with push server.
     */
    fun registerToken(requestObject: PushRegistrationRequest): Deferred<StatusResponse> {
        val gson = getGson()
        val typeAdapter = getTypeAdapter<PushRegistrationRequest>(gson)
        val bodyBytes = GsonRequestBodyBytes(gson, typeAdapter).convert(requestObject)
        val body = RequestBody.create(JSON_MEDIA_TYPE, bodyBytes)

        val requestBuilder = Request.Builder()
                .url(PUSH_URL)
                .post(body)

        val tokenHeader = tokenManager.getToken().generateHeader()
        requestBuilder.header(tokenHeader.key, tokenHeader.value)

        return makeCall(requestBuilder.build())
    }
}