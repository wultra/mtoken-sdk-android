/*
 * Copyright (c) 2023, Wultra s.r.o. (www.wultra.com).
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
 * Claim request model class.
 *
 * @property operationId Operation Id to be claimed.
 */
internal data class ClaimRequestObject(

    @SerializedName("operationId")
    val operationId: String
)