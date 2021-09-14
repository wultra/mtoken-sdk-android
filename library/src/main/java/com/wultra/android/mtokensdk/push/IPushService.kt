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

package com.wultra.android.mtokensdk.push

import com.wultra.android.powerauth.networking.error.ApiError

/**
 * Protocol for service, that communicates with Mobile Token API that handles registration for
 * push notifications.
 */
interface IPushService {
    /**
     * Accept language for the outgoing requests headers.
     * Default value is "en".
     */
    var acceptLanguage: String

    /**
     * Registers FCM on backend to receive notifications about operations
     * @param fcmToken Firebase Cloud Messaging Token
     * @param listener Result listener
     */
    fun register(fcmToken: String, listener: IPushRegisterListener)
}

/**
 * Listener for [IPushService.register] method.
 */
interface IPushRegisterListener {
    /**
     * Called when FCM token was registered on backend.
     */
    fun onSuccess()

    /**
     * Called when FCM token fails to register on backend.
     */
    fun onFailure(e: ApiError)
}