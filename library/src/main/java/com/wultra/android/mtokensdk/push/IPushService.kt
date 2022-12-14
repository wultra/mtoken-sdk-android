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
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.wultra.android.mtokensdk.push

import com.wultra.android.mtokensdk.api.apiErrorForListener
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
    fun register(fcmToken: String, callback: (result: Result<Unit>) -> Unit)
}
