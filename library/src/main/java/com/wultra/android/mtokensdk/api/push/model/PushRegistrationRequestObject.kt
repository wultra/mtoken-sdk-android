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

package com.wultra.android.mtokensdk.api.push.model

import com.google.gson.annotations.SerializedName

/**
 * Push registration request model class.
 */
data class PushRegistrationRequestObject(
        @SerializedName("token")
        val token: String) {

    @SerializedName("platform")
    val platform: String = "android"
}