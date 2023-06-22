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

import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor

/**
 * Interface for service that communicates with Inbox API that is managing user's inbox.
 */
interface IInboxService {
    /**
     * Accept language for the outgoing requests headers.
     * Default value is "en".
     *
     * Standard RFC "Accept-Language" https://tools.ietf.org/html/rfc7231#section-5.3.5
     * Response texts are based on this setting. For example when "de" is set, server
     * will return operation texts in german (if available).
     */
    var acceptLanguage: String

    /**
     * A custom interceptor can intercept each service call.
     *
     * You can use this for request/response logging into your own log system.
     */
    var okHttpInterceptor: OkHttpBuilderInterceptor?

    /**
     * Get number of unread messages in the inbox.
     *
     * @param callback Callback with result.
     */
    fun getUnreadCount(callback: (result: Result<InboxCount>) -> Unit)

    /**
     * Paged list of messages in the inbox. You can use also  [getAllMessages] method to fetch all messages.
     *
     * @param pageNumber Page number. First page is `0`, second `1`, etc.
     * @param pageSize Number of items received in the page.
     * @param onlyUnread If `true` then only unread messages will be returned.
     * @param callback Result callback. If the number of items in result is less than [pageSize] then the received page is the last page.
     */
    fun getMessageList(pageNumber: Int, pageSize: Int, onlyUnread: Boolean, callback: (result: Result<List<InboxMessage>>) -> Unit)

    /**
     * Get message detail in the inbox.
     *
     * @param messageId Message identifier.
     * @param callback Result callback.
     */
    fun getMessageDetail(messageId: String, callback: (result: Result<InboxMessageDetail>) -> Unit)

    /**
     * Mark the message with the given identifier as read.
     *
     * @param messageId Message identifier.
     * @param callback Result callback.
     */
    fun markRead(messageId: String, callback: (result: Result<Unit>) -> Unit)

    /**
     * Mark all unread messages in the inbox as read.
     *
     * @param callback Result callback.
     */
    fun markAllRead(callback: (result: Result<Unit>) -> Unit)
}

/**
 * Get all messages in the inbox. The function will issue multiple HTTP requests  until the list is not complete.
 *
 * @param onlyUnread If `true` then only unread messages will be returned. The default value is `false`.
 * @param pageSize How many messages should be fetched at once. The default value is 100.
 * @param messageLimit Maximum number of messages to be retrieved. Use `0` to set no limit. The default value is `1000`.
 * @param callback Callback with result.
 */
fun IInboxService.getAllMessages(onlyUnread: Boolean = false, pageSize: Int = 100, messageLimit: Int = 1000, callback: (result: Result<List<InboxMessage>>) -> Unit) {
    fetchPartialList(FetchOperation(0, pageSize, messageLimit, onlyUnread, callback))
}

/**
 * Fetch partial list from the server.
 * @param operation [FetchOperation] object.
 */
private fun IInboxService.fetchPartialList(operation: FetchOperation) {
    getMessageList(operation.pageNumber, operation.pageSize, operation.onlyUnread) { result ->
        result.onSuccess {
            if (operation.appendPartialMessages(it)) {
                operation.complete()
            } else {
                fetchPartialList(operation)
            }
        }.onFailure { operation.complete(result) }
    }
}

/**
 * Support class that keeps partially received messages and track the current page to fetch.
 */
private class FetchOperation(
    var pageNumber: Int,
    val pageSize: Int,
    val messageLimit: Int,
    val onlyUnread: Boolean,
    val completion: (Result<List<InboxMessage>>) -> Unit
) {
    val readMessages = mutableListOf<InboxMessage>()

    /**
     * Append received messages and determine whether we're at the end of the list.
     * @param messages Partial messages received from the server.
     * @return `true` if we're at the end of the list.
     */
    fun appendPartialMessages(messages: List<InboxMessage>): Boolean {
        readMessages.addAll(messages)
        pageNumber += 1
        return messages.size < pageSize || (messageLimit > 0 && readMessages.size >= messageLimit)
    }

    /**
     * Complete operation with result.
     * @param result Result to report back to the application. If `null` then success is reported.
     */
    fun complete(result: Result<List<InboxMessage>>? = null) {
        completion(result ?: Result.success(readMessages))
    }
}