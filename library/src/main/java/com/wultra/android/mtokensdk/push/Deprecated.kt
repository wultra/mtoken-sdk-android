/*
 * Copyright 2022 Wultra s.r.o.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package com.wultra.android.mtokensdk.push

import com.wultra.android.mtokensdk.api.apiErrorForListener
import com.wultra.android.powerauth.networking.error.ApiError

// Deprecated interface based API

/**
 * Listener for [IPushService.register] method.
 */
@Deprecated("Use function with Result<Unit> callback as a replacement") // 1.5.0
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

/**
 * Registers FCM on backend to receive notifications about operations
 * @param fcmToken Firebase Cloud Messaging Token
 * @param listener Result listener
 */
@Deprecated("Use function with Result<Unit> callback as a replacement") // 1.5.0
fun IPushService.register(fcmToken: String, listener: IPushRegisterListener) {
    register(fcmToken) { result ->
        result.onSuccess {
            listener.onSuccess()
        }.onFailure {
            listener.onFailure(it.apiErrorForListener())
        }
    }
}
