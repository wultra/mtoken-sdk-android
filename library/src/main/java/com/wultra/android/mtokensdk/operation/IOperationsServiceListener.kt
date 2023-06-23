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

import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.powerauth.networking.error.ApiError

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
