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

/**
 * Error codes denoting API errors.
 */
enum class ErrorCode(val message: String) {

    /* PUSH */
    ERROR_GENERIC("ERROR_GENERIC"),

    /*** webflow errors ***/

    PUSH_REGISTRATION_FAILED("PUSH_REGISTRATION_FAILED"),

    /* MOBILE TOKEN API */

    /// General authentication failure (wrong password, wrong activation state, etc...)
    POWERAUTH_AUTH_FAIL("POWERAUTH_AUTH_FAIL"),

    /// Invalid request sent - missing request object in request
    INVALID_REQUEST("INVALID_REQUEST"),

    /// Activation is not valid (it is different from configured activation)
    INVALID_ACTIVATION("INVALID_ACTIVATION"),

    /// Operation is already finished
    OPERATION_ALREADY_FINISHED("OPERATION_ALREADY_FINISHED"),

    /// Operation is already failed
    OPERATION_ALREADY_FAILED("OPERATION_ALREADY_FAILED"),

    /// Operation is cancelled
    OPERATION_ALREADY_CANCELED("OPERATION_ALREADY_CANCELED"),

    /// Operation is expired
    OPERATION_EXPIRED("OPERATION_EXPIRED"),

    /*** PowerAuth restful integration (enrollment) ***/

    ERR_ACTIVATION("ERR_ACTIVATION"),

    ERR_AUTHENTICATION("ERR_AUTHENTICATION"),

    ERR_SECURE_VAULT("ERR_SECURE_VAULT"),

    /**
     * Since crypto 3.0
     */
    ERR_ENCRYPTION("ERR_ENCRYPTION"),

    /**
     * Since crypto 3.0
     */
    ERR_UPGRADE("ERR_UPGRADE");

    companion object {
        private val map = mutableMapOf<String, ErrorCode>()

        init {
            values().forEach { ec -> map[ec.message] = ec }
        }

        fun errorCodeFromCodeString(code: String): ErrorCode? = map[code]
    }
}