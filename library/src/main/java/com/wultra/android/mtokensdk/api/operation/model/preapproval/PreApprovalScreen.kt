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

package com.wultra.android.mtokensdk.api.operation.model.preapproval

import com.google.gson.annotations.SerializedName

/**
 *  PreApprovalScreen contains data to be presented before approving operation
 *
 * `type` define different kind of data which can be passed with operation
 *  and shall be displayed before operation is confirmed
 */
open class PreApprovalScreen(

    /** Type of the PreApprovalScreen */
    @SerializedName("type")
    val type: Type,

    /** Heading of the pre-approval screen */
    @SerializedName("heading")
    val heading: String,

    /** Message to the user */
    @SerializedName("message")
    val message: String,

    /** Identifier of the screen */
    @SerializedName("id")
    val id: String? = null,

    /** Whether the back button should be visible */
    @SerializedName("backButton")
    val backButton: Boolean? = null,

    /** Image identifier */
    @SerializedName("image")
    val image: String? = null,

    /** Structured elements to display on the screen */
    @SerializedName("elements")
    val elements: List<PreApprovalElement>? = null,

    /** Approve/decline control specification */
    @SerializedName("controls")
    val controls: PreApprovalControls? = null
) {
    enum class Type(val value: String) {

        @SerializedName("INFO") INFO("INFO"),
        @SerializedName("WARNING") WARNING("WARNING"),
        @SerializedName("QR_SCAN") QR_SCAN("QR_SCAN"),
        UNKNOWN("UNKNOWN")
    }
}
