/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation.model

import com.google.gson.annotations.SerializedName

/**
 * Reject request model class - the wrapper requestObject.
 */
internal data class RejectRequest(
        @SerializedName("requestObject")
        val requestObject: RejectRequestObject)

/**
 * Reject request model class.
 *
 * @property id Operation ID.
 * @property reason Rejection reason.
 */
internal data class RejectRequestObject(
        @SerializedName("id")
        val id: String,

        @SerializedName("reason")
        val reason: String)