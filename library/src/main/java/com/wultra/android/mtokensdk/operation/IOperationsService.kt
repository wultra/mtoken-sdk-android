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

import com.wultra.android.mtokensdk.api.ResponseBodyConverter
import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.api.operation.model.OperationListResponse
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.api.operation.model.QROperation
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication

abstract class OperationsResult
data class SuccessOperationsResult(val operations: List<UserOperation>): OperationsResult()
data class ErrorOperationsResult(val error: ApiError): OperationsResult()

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

    /**
     * Last cached operation result for easy access.
     *
     * @return Last getOperations result. Null if not performed yet
     */
    fun getLastOperationsResult(): OperationsResult?

    /**
     * If operations are loading.
     */
    fun isLoadingOperations(): Boolean

    /**
     * Retrieves user operations and calls the listener when finished.
     *
     * @param listener Operation result listener
     */
    fun getOperations(listener: IGetOperationListener?)

    /**
     * Retrieves user operations, uses custom converter to parse to payload and calls the listener when finished.
     *
     * @param listener Operation result listener
     * @param customConverter Custom converter for payload parsing.
     */
    fun getOperations(customConverter: ResponseBodyConverter<OperationListResponse>, listener: IGetOperationListener?)

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
     * @param listener Result listener
     */
    fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener)

    /**
     * Rejects operation with provided reason
     *
     * @param operation Operation to reject
     * @param reason Rejection reason
     * @param listener Result listener
     */
    fun rejectOperation(operation: IOperation, reason: RejectionReason, listener: IRejectOperationListener)

    /**
     * Sign offline QR operation with provided authentication.
     *
     * @param operation Operation to approve
     * @param authentication PowerAuth authentication object
     *
     * @throws Exception Various exceptions, based on the error.
     *
     * @return Signature that should be displayed to the user
     */
    @Throws
    fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication): String
}