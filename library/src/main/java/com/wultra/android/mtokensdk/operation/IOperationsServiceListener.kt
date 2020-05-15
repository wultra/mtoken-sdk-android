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
import com.wultra.android.mtokensdk.api.operation.model.UserOperation


/**
 * Listener for operations loading events
 */
interface IOperationsServiceListener {
    /**
     * Called when operations has loaded.
     *
     * @param operations Loaded operations
     */
    fun operationsLoaded(operations: List<UserOperation>)

    /**
     * Called when operations loading changed
     *
     * @param loading If operations are loading
     */
    fun operationsLoading(loading: Boolean)

    /**
     * Called when operation loading fails
     *
     * @param error Error
     */
    fun operationsFailed(error: ApiError)
}