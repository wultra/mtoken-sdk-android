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

package com.wultra.android.mtokensdk.operation

import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.api.operation.model.QROperation
import com.wultra.android.powerauth.networking.error.ApiError
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication

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
     */
    var acceptLanguage: String

    val lastOperationsResult: Result<List<UserOperation>>?

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
 * Fetch operations from the server and report result to service's [listener]. The function is effective
 * only if service's listener is set.
 */
fun IOperationsService.fetchOperations() = getOperations {}