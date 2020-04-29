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
 * Describing behavior if the user should be prompted for a reason when rejecting an operation
 */
enum class OperationRejectReasonBehavior {
    /**
     * Always ask user why is he canceling the operation
     */
    ALWAYS_SHOW,
    /**
     * Never ask for reason when rejecting operation
     */
    NEVER_SHOW,
    /**
     * Let the server decide whether to show it or not (based on backend threat detection)
     */
    DYNAMIC
}