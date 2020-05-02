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

package com.wultra.android.mtokensdk.api.operation

import android.content.Context
import com.wultra.android.mtokensdk.api.Api
import com.wultra.android.mtokensdk.api.GsonRequestBodyBytes
import com.wultra.android.mtokensdk.api.IApiCallResponseListener
import com.wultra.android.mtokensdk.api.general.StatusResponse
import com.wultra.android.mtokensdk.api.operation.model.AuthorizeRequest
import com.wultra.android.mtokensdk.api.operation.model.OperationListResponse
import com.wultra.android.mtokensdk.api.operation.model.RejectRequest
import com.wultra.android.mtokensdk.common.IPowerAuthTokenListener
import com.wultra.android.mtokensdk.common.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import io.getlime.security.powerauth.sdk.PowerAuthToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

/**
 * API for operations requests.
 */
@Suppress("PrivatePropertyName")
internal class OperationApi constructor(okHttpClient: OkHttpClient,
                                        baseUrl: String,
                                        private val appContext: Context,
                                        private val tokenManager: IPowerAuthTokenProvider,
                                        private val powerAuthSDK: PowerAuthSDK) : Api(okHttpClient, baseUrl) {

    companion object {
        val AUTHORIZE_URL_ID = "/operation/authorize"
        val REJECT_URL_ID = "/operation/cancel"
        val OFFLINE_AUTHORIZE_URL_ID = "/operation/authorize/offline"
    }

    private val LIST_URL = constructApiUrl("api/auth/token/app/operation/list")
    private val AUTHORIZE_URL = constructApiUrl("api/auth/token/app/operation/authorize")
    private val REJECT_URL = constructApiUrl("api/auth/token/app/operation/cancel")

    /**
     * List pending operations.
     */
    fun list(listener: IApiCallResponseListener<OperationListResponse>) {
        val json: String = "{}"
        val body = RequestBody.create(JSON_MEDIA_TYPE, json)
        tokenManager.getTokenAsync(object : IPowerAuthTokenListener {
            override fun onReceived(token: PowerAuthToken) {
                val tokenHeader = token.generateHeader()
                val request = Request.Builder()
                        .url(LIST_URL)
                        .post(body)
                        .header("Accept-Language", acceptLanguage)
                        .header(tokenHeader.key, tokenHeader.value)
                        .build()
                return makeCall(request, listener)
            }

            override fun onFailed(e: Throwable) {
                listener.onFailure(e)
            }
        })
    }

    /**
     * Reject an operation.
     */
    fun reject(rejectRequest: RejectRequest, listener: IApiCallResponseListener<StatusResponse>) {
        val gson = getGson()
        val typeAdapter = getTypeAdapter<RejectRequest>(gson)
        val bodyBytes = GsonRequestBodyBytes(gson, typeAdapter).convert(rejectRequest)
        val authentication = PowerAuthAuthentication()
        authentication.usePossession = true
        val authorizationHeader = powerAuthSDK.requestSignatureWithAuthentication(appContext, authentication, "POST", REJECT_URL_ID, bodyBytes)
        val body = RequestBody.create(JSON_MEDIA_TYPE, bodyBytes)
        val request = Request.Builder()
                .url(REJECT_URL)
                .post(body)
                .header(authorizationHeader.key, authorizationHeader.value)
                .build()
        return makeCall(request, listener)
    }

    /**
     * Authorize an operation.
     */
    fun authorize(authorizeRequest: AuthorizeRequest, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<StatusResponse>) {
        val gson = getGson()
        val typeAdapter = getTypeAdapter<AuthorizeRequest>(gson)
        val bodyBytes = GsonRequestBodyBytes(gson, typeAdapter).convert(authorizeRequest)
        val authorizationHeader = powerAuthSDK.requestSignatureWithAuthentication(appContext, authentication, "POST", AUTHORIZE_URL_ID, bodyBytes)
        val body = RequestBody.create(JSON_MEDIA_TYPE, bodyBytes)
        val request = Request.Builder()
                .url(AUTHORIZE_URL)
                .post(body)
                .header(authorizationHeader.key, authorizationHeader.value)
                .build()
        return makeCall(request, listener)
    }
}