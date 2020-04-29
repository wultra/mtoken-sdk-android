/*
 * Copyright (c) 2018, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.operation

/**
 * Reason for rejecting a pending operation.
 *
 * @property reason Reason identifier.
 * @property radioButtonId Id of the designated radio button in UI.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
enum class RejectionReason(val reason: String) {

    /**
     * The data in the operation are incorrect.
     */
    INCORRECT_DATA("INCORRECT_DATA"),

    /**
     * The operation is unexpected. This might indicate a fraudulent operation.
     */
    UNEXPECTED_OPERATION("UNEXPECTED_OPERATION"),

    /**
     * The reason is unknown. User probably doesn't want to indicate the reason.
     */
    UNKNOWN("UNKNOWN")
}