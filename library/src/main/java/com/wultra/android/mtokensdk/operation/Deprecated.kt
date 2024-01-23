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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package com.wultra.android.mtokensdk.operation

import com.wultra.android.mtokensdk.api.apiErrorForListener
import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.operation.rejection.RejectionData
import com.wultra.android.mtokensdk.operation.rejection.RejectionReason
import com.wultra.android.powerauth.networking.error.ApiError
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication

@Deprecated("Use API function with Result<Unit> callback.") // 1.5.0
interface IAcceptOperationListener {
    fun onSuccess()
    fun onError(error: ApiError)
}

@Deprecated("Use API function with Result<Unit> callback.") // 1.5.0
interface IRejectOperationListener {
    fun onSuccess()
    fun onError(error: ApiError)
}

@Deprecated("Use API function with Result<List<UserOperation>> callback.") // 1.5.0
interface IGetOperationListener {
    fun onSuccess(operations: List<UserOperation>)
    fun onError(error: ApiError)
}

@Deprecated("Use API function with Result<List<OperationHistoryEntry>> callback.") // 1.5.0
interface IGetHistoryListener {
    fun onSuccess(operations: List<OperationHistoryEntry>)
    fun onError(error: ApiError)
}

// Deprecated interfaces and functions

@Deprecated("Use Result<List<UserOperation>> based API") // 1.5.0
abstract class OperationsResult

@Deprecated("Use Result<List<UserOperation>> based API") // 1.5.0
data class SuccessOperationsResult(val operations: List<UserOperation>): OperationsResult()

@Deprecated("Use Result<List<UserOperation>> based API") // 1.5.0
data class ErrorOperationsResult(val error: ApiError): OperationsResult()

/**
 * Last cached operation result for easy access.
 *
 * @return Last getOperations result. Null if not performed yet.
 */
@Deprecated("Use lastOperationsResult property", ReplaceWith("lastOperationsResult")) // 1.5.0
fun IOperationsService.getLastOperationsResult(): OperationsResult? {
    return lastOperationsResult?.let { result ->
        result.fold(
            onSuccess = { SuccessOperationsResult(it) },
            onFailure = { ErrorOperationsResult(it.apiErrorForListener()) }
        )
    }
}

/**
 * Retrieves user operations and calls the listener when finished.
 * @param listener Operation result listener
 */
@Deprecated("Use function with Result<List<UserOperation>> callback as a replacement") // 1.5.0
fun IOperationsService.getOperations(listener: IGetOperationListener?) {
    getOperations { result ->
        result.onSuccess {
            listener?.onSuccess(it)
        }.onFailure {
            listener?.onError(it.apiErrorForListener())
        }
    }
}

/**
 * Retrieves the history of user operations with its current status.
 *
 * @param authentication PowerAuth authentication object
 * @param listener Result listener
 */
@Deprecated("Use function with Result<List<OperationHistoryEntry>> callback as a replacement") // 1.5.0
fun IOperationsService.getHistory(authentication: PowerAuthAuthentication, listener: IGetHistoryListener) {
    getHistory(authentication) { result ->
        result.onSuccess {
            listener.onSuccess(it)
        }.onFailure {
            listener.onError(it.apiErrorForListener())
        }
    }
}

/**
 * Authorises operation with provided authentication
 *
 * @param operation Operation for approval
 * @param authentication PowerAuth authentication object
 * @param listener Result listener
 */
@Deprecated("Use function with Result<Unit> callback as a replacement") // 1.5.0
fun IOperationsService.authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener) {
    authorizeOperation(operation, authentication) { result ->
        result.onSuccess {
            listener.onSuccess()
        }.onFailure {
            listener.onError(it.apiErrorForListener())
        }
    }
}

/**
 * Rejects operation with provided reason
 *
 * @param operation Operation to reject
 * @param reason Rejection reason
 * @param listener Result listener
 */
@Deprecated("Use function with Result<Unit> callback as a replacement") // 1.5.0
fun IOperationsService.rejectOperation(operation: IOperation, reason: RejectionReason, listener: IRejectOperationListener) {
    rejectOperation(operation, reason) { result ->
        result.onSuccess {
            listener.onSuccess()
        }.onFailure {
            listener.onError(it.apiErrorForListener())
        }
    }
}

@Deprecated("Enum RejectionReason is deprecated. Use RejectionData wrapper object instead") // 1.8.3
fun IOperationsService.rejectOperation(operation: IOperation, reason: RejectionReason, callback: (result: Result<Unit>) -> Unit) {
    rejectOperation(operation, RejectionData(reason)) { result ->
        callback(result)
    }
}
