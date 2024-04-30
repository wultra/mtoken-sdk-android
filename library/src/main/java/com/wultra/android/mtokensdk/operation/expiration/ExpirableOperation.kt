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

package com.wultra.android.mtokensdk.operation.expiration

import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.log.WMTLogger
import org.threeten.bp.ZonedDateTime

/**
 * Interface defining an operation for the [OperationExpirationWatcher].
 */
interface ExpirableOperation {

    val expires: ZonedDateTime

    /**
     * Comparing method.
     * Default implementation is provided, but we suggest you to implement
     * your own logic.
     * @param other The other operation to check against
     */
    fun equals(other: ExpirableOperation): Boolean {
        return if (this is IOperation && other is IOperation) {
            id == other.id && data == other.data && expires == other.expires
        } else {
            WMTLogger.w("ExpirableOperation: Fallbacked to comparing `WMTExpirableOperation`s by reference.")
            this === other
        }
    }

    fun isExpired(currentDate: ZonedDateTime) = expires < currentDate
}
