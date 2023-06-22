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

package com.wultra.android.mtokensdk.inbox

import android.content.Context
import com.wultra.android.mtokensdk.api.inbox.*
import com.wultra.android.mtokensdk.api.inbox.model.GetList
import com.wultra.android.mtokensdk.api.inbox.model.GetMessageDetail
import com.wultra.android.mtokensdk.api.inbox.model.SetMessageRead
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

fun PowerAuthSDK.createInboxService(appContext: Context, baseURL: String, okHttpClient: OkHttpClient, userAgent: UserAgent? = null): IInboxService {
    return InboxService(okHttpClient, baseURL, this, appContext, null, userAgent)
}

fun PowerAuthSDK.createInboxService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy, userAgent: UserAgent? = null): IInboxService {
    val builder = OkHttpClient.Builder()
    strategy.configure(builder)
    Logger.configure(builder)
    return createInboxService(appContext, baseURL, builder.build(), userAgent)
}

class InboxService(
    httpClient: OkHttpClient,
    baseURL: String,
    powerAuthSDK: PowerAuthSDK,
    appContext: Context,
    tokenProvider: IPowerAuthTokenProvider? = null,
    userAgent: UserAgent? = null) : IInboxService {

    // API class for communication.
    private val inboxApi = InboxApi(httpClient, baseURL, appContext, powerAuthSDK, tokenProvider, userAgent, OperationsUtils.defaultGsonBuilder())

    override var acceptLanguage: String
        get() = inboxApi.acceptLanguage
        set(value) {
            inboxApi.acceptLanguage = value
        }

    override var okHttpInterceptor: OkHttpBuilderInterceptor?
        get() = inboxApi.okHttpInterceptor
        set(value) {
            inboxApi.okHttpInterceptor = value
        }

    override fun getUnreadCount(callback: (result: Result<InboxCount>) -> Unit) {
        inboxApi.count(object: IApiCallResponseListener<InboxCountResponse> {
            override fun onFailure(error: ApiError) {
                callback(Result.failure(ApiErrorException(error)))
            }

            override fun onSuccess(result: InboxCountResponse) {
                callback(Result.success(result.responseObject))
            }
        })
    }

    override fun getMessageList(pageNumber: Int, pageSize: Int, onlyUnread: Boolean, callback: (result: Result<List<InboxMessage>>) -> Unit) {
        inboxApi.list(InboxGetListRequest(GetList(pageNumber, pageSize, onlyUnread)), object : IApiCallResponseListener<InboxGetListResponse> {
            override fun onFailure(error: ApiError) {
                callback(Result.failure(ApiErrorException(error)))
            }

            override fun onSuccess(result: InboxGetListResponse) {
                callback(Result.success(result.responseObject))
            }
        })
    }

    override fun getMessageDetail(messageId: String, callback: (result: Result<InboxMessageDetail>) -> Unit) {
        inboxApi.detail(InboxGetMessageDetailRequest(GetMessageDetail(messageId)), object : IApiCallResponseListener<InboxGetMessageDetailResponse> {
            override fun onFailure(error: ApiError) {
                callback(Result.failure(ApiErrorException(error)))
            }

            override fun onSuccess(result: InboxGetMessageDetailResponse) {
                callback(Result.success(result.responseObject))
            }
        })
    }

    override fun markRead(messageId: String, callback: (result: Result<Unit>) -> Unit) {
        inboxApi.read(InboxSetMessageReadRequest(SetMessageRead(messageId)), object : IApiCallResponseListener<StatusResponse> {
            override fun onFailure(error: ApiError) {
                callback(Result.failure(ApiErrorException(error)))
            }

            override fun onSuccess(result: StatusResponse) {
                callback(Result.success(Unit))
            }
        })
    }

    override fun markAllRead(callback: (result: Result<Unit>) -> Unit) {
        inboxApi.readAll(object : IApiCallResponseListener<StatusResponse> {
            override fun onFailure(error: ApiError) {
                callback(Result.failure(ApiErrorException(error)))
            }

            override fun onSuccess(result: StatusResponse) {
                callback(Result.success(Unit))
            }
        })
    }
}