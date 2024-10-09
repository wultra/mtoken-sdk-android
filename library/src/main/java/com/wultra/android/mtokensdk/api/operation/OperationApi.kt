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

package com.wultra.android.mtokensdk.api.operation

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.powerauth.networking.*
import com.wultra.android.powerauth.networking.data.*
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient
import org.threeten.bp.ZonedDateTime

internal class OperationListResponse(
    @SerializedName("currentTimestamp")
    val currentTimestamp: ZonedDateTime?,
    responseObject: List<UserOperation>,
    status: Status
): ObjectResponse<List<UserOperation>>(responseObject, status)
internal class OperationHistoryResponse(responseObject: List<UserOperation>, status: Status): ObjectResponse<List<UserOperation>>(responseObject, status)
internal class AuthorizeRequest(requestObject: AuthorizeRequestObject): ObjectRequest<AuthorizeRequestObject>(requestObject)
internal class RejectRequest(requestObject: RejectRequestObject): ObjectRequest<RejectRequestObject>(requestObject)
internal class OperationClaimDetailRequest(requestObject: OperationClaimDetailData): ObjectRequest<OperationClaimDetailData>(requestObject)
internal class OperationClaimDetailResponse(responseObject: UserOperation, status: Status): ObjectResponse<UserOperation>(responseObject, status)

/**
 * API for operations requests.
 */
internal class OperationApi(
    okHttpClient: OkHttpClient,
    baseUrl: String,
    appContext: Context,
    powerAuthSDK: PowerAuthSDK,
    tokenProvider: IPowerAuthTokenProvider?,
    userAgent: UserAgent?,
    gsonBuilder: GsonBuilder?
) : Api(baseUrl, okHttpClient, powerAuthSDK, gsonBuilder ?: OperationsUtils.defaultGsonBuilder(), appContext, tokenProvider, userAgent ?: UserAgent.libraryDefault(appContext)) {

    private object EmptyRequest: BaseRequest()

    companion object {
        private val historyEndpoint = EndpointSigned<EmptyRequest, OperationHistoryResponse>("api/auth/token/app/operation/history", "/operation/history")
        private val listEndpoint = EndpointSignedWithToken<EmptyRequest, OperationListResponse>("api/auth/token/app/operation/list", "possession_universal")
        private val authorizeEndpoint = EndpointSigned<AuthorizeRequest, StatusResponse>("api/auth/token/app/operation/authorize", "/operation/authorize")
        private val rejectEndpoint = EndpointSigned<RejectRequest, StatusResponse>("api/auth/token/app/operation/cancel", "/operation/cancel")
        private val detailEndpoint = EndpointSignedWithToken<OperationClaimDetailRequest, OperationClaimDetailResponse>("api/auth/token/app/operation/detail", "possession_universal")
        private val claimEndpoint = EndpointSignedWithToken<OperationClaimDetailRequest, OperationClaimDetailResponse>("api/auth/token/app/operation/detail/claim", "possession_universal")
        const val OFFLINE_AUTHORIZE_URI_ID = "/operation/authorize/offline"
    }

    var okHttpInterceptor: OkHttpBuilderInterceptor? = null

    /** List pending operations. */
    fun list(listener: IApiCallResponseListener<OperationListResponse>) {
        post(EmptyRequest, listEndpoint, null, okHttpInterceptor, listener)
    }

    /** Retrieves operation history */
    fun history(authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<OperationHistoryResponse>) {
        post(EmptyRequest, historyEndpoint, authentication, null, okHttpInterceptor, listener)
    }

    /** Reject an operation. */
    fun reject(rejectRequest: RejectRequest, listener: IApiCallResponseListener<StatusResponse>) {
        val authentication = PowerAuthAuthentication.possession()
        post(rejectRequest, rejectEndpoint, authentication, null, okHttpInterceptor, listener)
    }

    /** Authorize an operation. */
    fun authorize(authorizeRequest: AuthorizeRequest, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<StatusResponse>) {
        post(authorizeRequest, authorizeEndpoint, authentication, null, okHttpInterceptor, listener)
    }

    /** Get an operation detail. */
    fun getDetail(claimRequest: OperationClaimDetailRequest, listener: IApiCallResponseListener<OperationClaimDetailResponse>) {
        post(data = claimRequest, endpoint = detailEndpoint, headers = null, okHttpInterceptor = okHttpInterceptor, listener = listener)
    }

    /** Claim an operation. */
    fun claim(claimRequest: OperationClaimDetailRequest, listener: IApiCallResponseListener<OperationClaimDetailResponse>) {
        post(data = claimRequest, endpoint = claimEndpoint, headers = null, okHttpInterceptor = okHttpInterceptor, listener = listener)
    }
}
