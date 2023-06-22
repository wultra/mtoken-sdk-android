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

package com.wultra.android.mtokensdk.operation

import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import com.wultra.android.mtokensdk.api.operation.model.QROperation
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import org.threeten.bp.ZonedDateTime

/**
 * Service for operations handling.
 */
interface IOperationsService {

    /**
     * Listener gets notified about changes in operations loading and its result.
     */
    var listener: IOperationsServiceListener?

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

    val lastOperationsResult: Result<List<UserOperation>>?

    /**
     * Current server date
     *
     * This is calculated property based on the difference between phone date
     * and date on the server.
     *
     * This property is available after the first successful operation list request.
     * It might be nil if the server doesn't provide such a feature.
     *
     * Note that this value might be incorrect when the user decide to
     * change the system time during the runtime of the application.
     */
    fun currentServerDate(): ZonedDateTime?

    /**
     * If operations are loading.
     */
    fun isLoadingOperations(): Boolean

    /**
     * Retrieves user operations.
     *
     * @param callback Callback with result
     */
    fun getOperations(callback: (result: Result<List<UserOperation>>) -> Unit)

    /**
     * Retrieves the history of user operations with its current status.
     *
     * @param authentication PowerAuth authentication object
     * @param callback Callback with result.
     */
    fun getHistory(authentication: PowerAuthAuthentication, callback: (result: Result<List<OperationHistoryEntry>>) -> Unit)

    /**
     * Returns if operation polling is running
     */
    fun isPollingOperations(): Boolean

    /**
     * Starts polling operations from the server. You can observe the polling via [listener].
     *
     * If operations are already polling, this call is ignored
     * and the polling interval won't be changed.
     *
     * @param pollingInterval Polling interval in milliseconds
     * @param delayStart When true, polling starts after the first [pollingInterval] time passes
     */
    fun startPollingOperations(pollingInterval: Long, delayStart: Boolean)

    /**
     * Stops operation polling
     */
    fun stopPollingOperations()

    /**
     * Authorises operation with provided authentication
     *
     * @param operation Operation for approval
     * @param authentication PowerAuth authentication object
     * @param callback Callback with result.
     */
    fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, callback: (result: Result<Unit>) -> Unit)

    /**
     * Rejects operation with provided reason
     *
     * @param operation Operation to reject
     * @param reason Rejection reason
     * @param callback Callback with result.
     */
    fun rejectOperation(operation: IOperation, reason: RejectionReason, callback: (result: Result<Unit>) -> Unit)

    /**
     * Sign offline QR operation with provided authentication.
     *
     * @param operation Operation to approve
     * @param authentication PowerAuth authentication object
     * @param uriId uriId: Custom signature URI ID of the operation. Use URI ID under which the operation was
     * created on the server. Default value is `/operation/authorize/offline`.
     *
     * @throws Exception Various exceptions, based on the error.
     *
     * @return Signature that should be displayed to the user
     */
    @Throws
    fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication, uriId: String = OperationApi.OFFLINE_AUTHORIZE_URI_ID): String
}

/**
 * Fetch operations from the server and report result to service's [IOperationsService.listener]. The function is effective
 * only if service's listener is set.
 */
fun IOperationsService.fetchOperations() = getOperations {}
