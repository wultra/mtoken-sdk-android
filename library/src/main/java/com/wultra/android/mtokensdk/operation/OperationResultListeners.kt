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

import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.powerauth.networking.error.ApiError

interface IAcceptOperationListener {
    fun onSuccess()
    fun onError(error: ApiError)
}

interface IRejectOperationListener {
    fun onSuccess()
    fun onError(error: ApiError)
}

interface IGetOperationListener {
    fun onSuccess(operations: List<UserOperation>)
    fun onError(error: ApiError)
}

interface IGetHistoryListener {
    fun onSuccess(operations: List<OperationHistoryEntry>)
    fun onError(error: ApiError)
}