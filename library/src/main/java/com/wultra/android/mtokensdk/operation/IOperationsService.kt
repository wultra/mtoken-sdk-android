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

import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.operation.model.Operation
import com.wultra.android.mtokensdk.api.operation.model.QROperation
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication

abstract class OperationsResult
data class SuccessOperationsResult(val operations: List<Operation>): OperationsResult()
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
     * Returns if operation polling is running
     */
    fun isPollingOperations(): Boolean

    /**
     * Starts polling operations from the server. You can observe the polling via [listener].
     *
     * @param pollingInterval Polling interval in milliseconds
     */
    fun startPollingOperations(pollingInterval: Long)

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
    fun authorizeOperation(operation: Operation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener)

    /**
     * Rejects operation with provided reason
     *
     * @param operation Operation to reject
     * @param reason Rejection reason
     * @param listener Result listener
     */
    fun rejectOperation(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener)

    /**
     * Sign offline QR operation with biometry.
     *
     * @param biometry Biometry data
     * @param offlineOperation Operation to approve
     *
     * @return Signature. Null if the signing failed
     */
    fun signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation): String?
    /**
     * Sign offline QR operation with password.
     *
     * @param password Password to for signing
     * @param offlineOperation Operation to approve
     *
     * @return Signature. Null if the signing failed
     */
    fun signOfflineOperationWithPassword(password: String, offlineOperation: QROperation): String?

    /**
     * Process loaded payload from a scanned offline QR.
     *
     * @param payload String parsed from QR code
     *
     * @throws IllegalArgumentException When there is no operation in provided payload.
     * @return Parsed operation.
     */
    @Throws(IllegalArgumentException::class)
    fun processOfflineQrPayload(payload: String): QROperation?
}