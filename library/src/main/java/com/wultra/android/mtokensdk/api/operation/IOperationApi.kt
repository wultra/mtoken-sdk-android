/*
 * Copyright 2025 Wultra s.r.o.
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

import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.data.StatusResponse
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication

/**
 * Interface for operations API requests.
 */
internal interface IOperationApi {

    var acceptLanguage: String

    var okHttpInterceptor: OkHttpBuilderInterceptor?

    /** List pending operations. */
    fun list(listener: IApiCallResponseListener<OperationListResponse>)

    /** Retrieves operation history. */
    fun history(authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<OperationHistoryResponse>)

    /** Reject an operation. */
    fun reject(rejectRequest: RejectRequest, listener: IApiCallResponseListener<StatusResponse>)

    /** Authorize an operation. */
    fun authorize(authorizeRequest: AuthorizeRequest, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<StatusResponse>)

    /** Get an operation detail. */
    fun getDetail(claimRequest: OperationClaimDetailRequest, listener: IApiCallResponseListener<OperationClaimDetailResponse>)

    /** Claim an operation. */
    fun claim(claimRequest: OperationClaimDetailRequest, listener: IApiCallResponseListener<OperationClaimDetailResponse>)
}
