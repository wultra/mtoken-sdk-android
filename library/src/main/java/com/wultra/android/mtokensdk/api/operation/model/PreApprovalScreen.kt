/*
 * Copyright 2023 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.api.operation.model

/**
 *  PreApprovalScreen contains data to be presented before approving operation
 *
 * `type` define different kind of data which can be passed with operation
 *  and shall be displayed before operation is confirmed
 */
open class PreApprovalScreen(
        /**
         * Type of PreApprovalScreen (`WARNING`, `INFO`, `QR_SCAN` or `UNKNOWN` for future compatibility)
         */
        val type: Type,

        /**
         * Heading of the pre-approval screen
         */
        val heading: String,

        /**
         * Message to the user
         */
        val message: String,

        /**
         * Array of items to be displayed as list of choices
         */
        val items: List<String>?,

        /**
         * Type of the approval button
         */
        val approvalType: PreApprovalScreenConfirmAction?) {
    enum class Type(val value: String) {
        INFO("INFO"),
        WARNING("WARNING"),
        QR_SCAN("QR_SCAN"),
        UNKNOWN("UNKNOWN")
    }
}

enum class PreApprovalScreenConfirmAction {
    SLIDER
}
