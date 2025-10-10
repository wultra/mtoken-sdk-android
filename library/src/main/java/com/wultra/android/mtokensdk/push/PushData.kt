/*
 * Copyright 2024 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
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
