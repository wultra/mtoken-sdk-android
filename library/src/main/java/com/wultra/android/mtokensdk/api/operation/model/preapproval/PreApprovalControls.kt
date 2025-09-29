/*
 * Copyright 2025 Wultra s.r.o.
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
