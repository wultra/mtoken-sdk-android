/*
 * Copyright (c) 2018, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.general

import com.wultra.android.mtokensdk.api.ErrorCode
import com.wultra.android.mtokensdk.api.HttpException
import io.getlime.security.powerauth.networking.exceptions.ErrorResponseApiException
import java.net.ConnectException

/**
 * Container class for general API failure.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
data class GeneralFailure(val e: Throwable) {

    val error: ErrorCode?
    var remainingAttempts: Int? = null

    init {
        if (e is HttpException) {
            error = e.errorResponse?.responseObject?.errorCode
        } else if (e is ErrorResponseApiException) {
            error = ErrorCode.errorCodeFromCodeString(e.errorResponse.code)
        } else {
            error = null
        }
    }

    fun isOffline(): Boolean {
        return e is ConnectException
    }
}