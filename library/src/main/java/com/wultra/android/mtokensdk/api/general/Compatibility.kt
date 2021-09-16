/*
 * Copyright (c) 2021, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.general

import com.wultra.android.powerauth.networking.error.ApiError

// Type aliases to for easier migration

@Deprecated(
    "This class was moved to different package (com.wultra.android.powerauth.networking.error).",
    ReplaceWith("com.wultra.android.powerauth.networking.error.ApiError"))
typealias ApiError = ApiError