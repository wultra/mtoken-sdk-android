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
import org.threeten.bp.ZonedDateTime

/**
 * Authorize request model class.
 *
 * @property id Operation ID.
 * @property data Operation data.
 * @property proximityCheck Proximity check OTP data.
 */
internal data class AuthorizeRequestObject(
    @SerializedName("id")
    val id: String,

    @SerializedName("data")
    val data: String,

    @SerializedName("proximityCheck")
    val proximityCheck: ProximityCheckData? = null
) {

    constructor(operation: IOperation, timestampSent: ZonedDateTime = ZonedDateTime.now()): this(
        operation.id,
        operation.data,
        operation.proximityCheck?.let {
            ProximityCheckData(
                it.totp,
                it.type,
                it.timestampReceived,
                timestampSent
            )
        }
    )
}

internal data class ProximityCheckData(

    /** The actual OTP code */
    @SerializedName("otp")
    val otp: String,

    /** Type of the Proximity check */
    @SerializedName("type")
    val type: ProximityCheckType,

    /** Timestamp when the operation was received by the */
    @SerializedName("timestampReceived")
    val timestampReceived: ZonedDateTime,

    /** Timestamp when the operation was sent */
    @SerializedName("timestampSent")
    val timestampSent: ZonedDateTime
)
