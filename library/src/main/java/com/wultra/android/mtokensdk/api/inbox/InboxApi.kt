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

package com.wultra.android.mtokensdk.api.inbox

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.inbox.model.GetList
import com.wultra.android.mtokensdk.api.inbox.model.GetMessageDetail
import com.wultra.android.mtokensdk.api.inbox.model.SetMessageRead
import com.wultra.android.mtokensdk.inbox.InboxCount
import com.wultra.android.mtokensdk.inbox.InboxMessage
import com.wultra.android.mtokensdk.inbox.InboxMessageDetail
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.powerauth.networking.Api
import com.wultra.android.powerauth.networking.EndpointSignedWithToken
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.BaseRequest
import com.wultra.android.powerauth.networking.data.ObjectRequest
import com.wultra.android.powerauth.networking.data.ObjectResponse
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

internal class InboxCountResponse(responseObject: InboxCount, status: Status): ObjectResponse<InboxCount>(responseObject, status)
internal class InboxGetListRequest(requestObject: GetList): ObjectRequest<GetList>(requestObject)
internal class InboxGetListResponse(responseObject: List<InboxMessage>, status: Status): ObjectResponse<List<InboxMessage>>(responseObject, status)
internal class InboxGetMessageDetailRequest(requestObject: GetMessageDetail): ObjectRequest<GetMessageDetail>(requestObject)
internal class InboxGetMessageDetailResponse(responseObject: InboxMessageDetail, status: Status): ObjectResponse<InboxMessageDetail>(responseObject, status)
internal class InboxSetMessageReadRequest(requestObject: SetMessageRead): ObjectRequest<SetMessageRead>(requestObject)

internal class InboxApi(okHttpClient: OkHttpClient,
                        baseUrl: String,
                        appContext: Context,
                        powerAuthSDK: PowerAuthSDK,
                        tokenProvider: IPowerAuthTokenProvider?,
                        userAgent: UserAgent?,
                        gsonBuilder: GsonBuilder?) : Api(baseUrl, okHttpClient, powerAuthSDK, gsonBuilder ?: OperationsUtils.defaultGsonBuilder(), appContext, tokenProvider, userAgent ?: UserAgent.libraryDefault(appContext)) {

    companion object {
        private val getMessageCount = EndpointSignedWithToken<BaseRequest, InboxCountResponse>("api/inbox/count", "possession_universal")
        private val getMessageList = EndpointSignedWithToken<InboxGetListRequest, InboxGetListResponse>("api/inbox/message/list", "possession_universal")
        private val getMessageDetail = EndpointSignedWithToken<InboxGetMessageDetailRequest, InboxGetMessageDetailResponse>("api/inbox/message/detail", "possession_universal")
        private val setMessageRead = EndpointSignedWithToken<InboxSetMessageReadRequest, StatusResponse>("api/inbox/message/read", "possession_universal")
        private val setMessageAllRead = EndpointSignedWithToken<BaseRequest, StatusResponse>("api/inbox/message/read-all", "possession_universal")
    }

    /**
     * Get count of unread messages.
     */
    fun count(listener: IApiCallResponseListener<InboxCountResponse>) {
        post(BaseRequest(), getMessageCount, null, null, listener)
    }

    /**
     * Get paged message list.
     */
    fun list(request: InboxGetListRequest, listener: IApiCallResponseListener<InboxGetListResponse>) {
        post(request, getMessageList, null, null, listener)
    }

    /**
     * Get message detail.
     */
    fun detail(request: InboxGetMessageDetailRequest, listener: IApiCallResponseListener<InboxGetMessageDetailResponse>) {
        post(request, getMessageDetail, null, null, listener)
    }

    /**
     * Set message as read.
     */
    fun read(request: InboxSetMessageReadRequest, listener: IApiCallResponseListener<StatusResponse>) {
        post(request, setMessageRead, null, null, listener)
    }

    /**
     * Set all messages as read.
     */
    fun readAll(listener: IApiCallResponseListener<StatusResponse>) {
        post(BaseRequest(), setMessageAllRead, null, null, listener)
    }
}
