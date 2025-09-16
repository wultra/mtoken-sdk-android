/*
 * Copyright (c) 2025, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation.model.preapproval

/**
 * Defines approve/decline control configuration on the screen.
 */
data class PreApprovalControls(

    /**
     * Whether to flip the order of approve/decline buttons.
     */
    val flip: Boolean? = null,

    /**
     * Axis for arranging buttons.
     */
    val axis: ButtonAxis? = null,

    /**
     * Specification of the decline control.
     */
    val decline: Decline? = null,

    /**
     * Specification of the approve control.
     */
    val approve: Approve? = null,
) {

    /**
     * Decline control specification.
     */
    data class Decline(

        /** Type of decline action (`BACK` or `REJECT`). */
        val type: DeclineType? = null,

        /** Text for the decline button. */
        val text: String? = null
    )

    /**
     * Approve control specification.
     */
    data class Approve(

        /** Type of approve action (`SLIDER` or `BUTTON`). */
        val type: ApproveType? = null,

        /** Text for the approve control. */
        val text: String? = null,

        /** Countdown timer (in seconds) before enabling the approve control. */
        val counter: Int? = null
    )

    /** Axis for arranging controls. */
    enum class ButtonAxis { HORIZONTAL, VERTICAL }

    /** Decline action types. */
    enum class DeclineType { BACK, REJECT }

    /** Approve action types. */
    enum class ApproveType { BUTTON, SLIDER }
}
