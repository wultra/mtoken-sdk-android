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
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.powerauth.networking.*
import com.wultra.android.powerauth.networking.data.*
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient
import org.threeten.bp.ZonedDateTime

internal class OperationListResponse(@SerializedName("responseObject") val responseObject: List<UserOperation>, status: Status): StatusResponse(status)
internal class OperationHistoryResponse(@SerializedName("responseObject") val responseObject: List<OperationHistoryEntry>, status: Status): StatusResponse(status)
internal class AuthorizeRequest(@SerializedName("requestObject") val requestObject: AuthorizeRequestObject)
internal class RejectRequest(@SerializedName("requestObject") val requestObject: RejectRequestObject)

/**
 * API for operations requests.
 */
@Suppress("PrivatePropertyName")
internal class OperationApi(okHttpClient: OkHttpClient,
                            baseUrl: String,
                            appContext: Context,
                            powerAuthSDK: PowerAuthSDK,
                            tokenProvider: IPowerAuthTokenProvider?) : Api(baseUrl, okHttpClient, powerAuthSDK, getGson(), appContext, tokenProvider) {

    private class EmptyRequest

    companion object {
        private val historyEndpoint = EndpointSigned<EmptyRequest, OperationHistoryResponse>("api/auth/token/app/operation/history", "/operation/history")
        private val listEndpoint = EndpointSignedWithToken<EmptyRequest, OperationListResponse>("api/auth/token/app/operation/list", "possession_universal")
        private val authorizeEndpoint = EndpointSigned<AuthorizeRequest, StatusResponse>("api/auth/token/app/operation/authorize", "/operation/authorize")
        private val rejectEndpoint = EndpointSigned<RejectRequest, StatusResponse>("api/auth/token/app/operation/cancel", "/operation/cancel")
        const val OFFLINE_AUTHORIZE_URI_ID = "/operation/authorize/offline"

        private fun getGson(): GsonBuilder {
            val builder = GsonBuilder()
            builder.registerTypeHierarchyAdapter(Attribute::class.java, AttributeTypeAdapter())
            builder.registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeDeserializer())
            builder.registerTypeAdapter(OperationHistoryEntry::class.java, OperationHistoryEntryDeserializer())
            return builder
        }
    }

    /** List pending operations. */
    fun list(listener: IApiCallResponseListener<OperationListResponse>) {
        post(EmptyRequest(), listEndpoint, null, null, listener)
    }

    /** Retrieves operation history */
    fun history(authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<OperationHistoryResponse>) {
        post(EmptyRequest(), historyEndpoint, authentication, null, null, listener)
    }

    /** Reject an operation. */
    fun reject(rejectRequest: RejectRequest, listener: IApiCallResponseListener<StatusResponse>) {
        val authentication = PowerAuthAuthentication()
        authentication.usePossession = true
        post(rejectRequest, rejectEndpoint, authentication, null, null, listener)
    }

    /** Authorize an operation. */
    fun authorize(authorizeRequest: AuthorizeRequest, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<StatusResponse>) {
        post(authorizeRequest, authorizeEndpoint, authentication, null, null, listener)
    }
}