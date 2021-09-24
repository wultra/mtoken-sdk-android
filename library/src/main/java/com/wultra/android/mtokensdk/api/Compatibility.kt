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

@file:Suppress("unused")

package com.wultra.android.mtokensdk.api

import com.wultra.android.powerauth.networking.error.ApiErrorCode
import com.wultra.android.powerauth.networking.error.ApiHttpException

// Type aliases to for easier migration

@Deprecated(
    "This enum was moved and renamed.",
    ReplaceWith("com.wultra.android.powerauth.networking.error.ApiErrorCode"))
typealias MTokenErrorCode = ApiErrorCode

@Deprecated(
    "This class was moved and renamed.",
    ReplaceWith("com.wultra.android.powerauth.networking.error.ApiHttpException"))
typealias MTokenHttpException = ApiHttpException