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

package com.wultra.android.mtokensdk.api

import com.wultra.android.mtokensdk.api.general.ErrorResponse
import okhttp3.Response

/**
 * Exception for describing HTTP exceptions.
 */
internal class HttpException(response: Response, val errorResponse: ErrorResponse? = null) : RuntimeException(getErrorMessage(response)) {

    companion object {
        private fun getErrorMessage(response: Response): String {
            return "HTTP " + response.code() + " " + response.message()
        }
    }

    val code: Int = response.code()
    override val message: String = response.message()

}