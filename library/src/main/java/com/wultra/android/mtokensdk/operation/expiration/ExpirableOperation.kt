/*
 * Copyright (c) 2021, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.operation.expiration

import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.common.Logger
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
            Logger.w("ExpirableOperation: Fallbacked to comparing `WMTExpirableOperation`s by reference.")
            this === other
        }
    }

    fun isExpired(currentDate: ZonedDateTime) = expires < currentDate
}