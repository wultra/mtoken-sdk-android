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

package com.wultra.android.mtokensdk.api.operation.model

import com.google.gson.annotations.SerializedName

/**
 * Reject request model class.
 */
internal data class RejectRequestObject(

    /** Operation ID */
    @SerializedName("id")
    val id: String,

    /** Rejection reason */
    @SerializedName("reason")
    val reason: String,

    /** Additional mobile token data for rejection (available with PowerAuth server 1.10+) */
    @SerializedName("mobileTokenData")
    val mobileTokenData: Map<String, Any>? = null
)
