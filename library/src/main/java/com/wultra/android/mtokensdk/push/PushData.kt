/*
 * Copyright (c) 2024, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.push

/**
 * Push data to register.
 *
 * @property token Token received from the Push Provider
 * @property platform Push Provider (Firebase Cloud Messaging for Android)
 */
data class PushData(
    val token: String,
    val platform: PushPlatform
) {
    companion object {
        /**
         * Creates data for Firebase Cloud Messaging.
         *
         * @param token FCM token
         */
        @JvmStatic fun fcm(token: String) = PushData(token, PushPlatform.FCM)

        /**
         * Creates data for Huawei Messaging Service.
         *
         * @param token HMS token
         */
        @JvmStatic fun hms(token: String) = PushData(token, PushPlatform.HMS)
    }
}

/**
 * Push provider which you're registering to.
 */
enum class PushPlatform {
    /** Firebase Cloud Messaging */
    FCM,
    /** Huawei Messaging Service */
    HMS
}
