/*
 * Copyright 2025 Wultra s.r.o.
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

package com.wultra.android.mtokensdk

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.inbox.InboxService
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.oidc.OidcService
import com.wultra.android.mtokensdk.operation.OperationsService
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.mtokensdk.push.PushService
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Convenience factory method to create a WultraMobileToken instance
 * from given PowerAuthSDK instance from default values.
 *
 * @param appContext Application Context
 * @param okHttpClient HTTP client instance for networking
 * @param acceptLanguage The language code to set for the `Accept-Language` header.
 * @param userAgent Default user agent for each request.
 * @param gsonBuilder Custom GSON builder for deserialization of request. If you want to provide your own
 * deserialization logic, we recommend adding it to the instance obtained from the [OperationsUtils.defaultGsonBuilder].
 * @return WultraMobileToken instance
 */
fun PowerAuthSDK.createWultraMobileToken(
    appContext: Context,
    okHttpClient: OkHttpClient = WultraMobileToken.defaultOkHttpClient(),
    acceptLanguage: String = "en",
    userAgent: UserAgent = UserAgent.libraryDefault(appContext),
    gsonBuilder: GsonBuilder? = null
): WultraMobileToken {
    return WultraMobileToken(
        appContext,
        this,
        okHttpClient,
        acceptLanguage,
        userAgent,
        gsonBuilder
    )
}

/**
 * WultraMobileToken is a centralized class responsible for initializing and providing access
 * to multiple SDK services. This class simplifies the setup and usage of the following components:
 *
 * @param appContext Application Context
 * @param powerAuthSDK PowerAuth instance
 * @param okHttpClient OkHttpClient for API communication
 * @param acceptLanguage The language code to set for the `Accept-Language` header.
 * @param userAgent Default user agent used for each request.
 * @param gsonBuilder GSON builder for deserialization of request.
 *
 * - [OperationsService]: Handles operations and workflows.
 * - [PushService]: Manages push notifications and related tasks.
 * - [InboxService]: Provides functionality for accessing and managing inbox messages.
 *
 * - **Extensible Configuration**: Supports custom `NetworkingConfig` for each service, enabling tailored setups.
 */
class WultraMobileToken(
    private val appContext: Context,
    private val powerAuthSDK: PowerAuthSDK,
    private val okHttpClient: OkHttpClient = defaultOkHttpClient(),
    private var acceptLanguage: String = "en",
    private val userAgent: UserAgent = UserAgent.libraryDefault(appContext),
    private val gsonBuilder: GsonBuilder? = null
) {

    /**
     * Lazily initialized service for operations handling.
     */
    val operations: OperationsService by lazy { createOperations() }

    /**
     * Lazily initialized service for push notification registering
     */
    val push: PushService by lazy { createPush() }

    /**
     * Lazily initialized service for managing user's inbox
     */
    val inbox: InboxService by lazy { createInbox() }

    /**
     * Lazily initialized service for managing oidc flow preparation and activation
     */
    val oidc: OidcService by lazy { createOidc() }

    /**
     * Default OkHttpClient
     */
    companion object {
        fun defaultOkHttpClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
            val strategy = SSLValidationStrategy.system()
            strategy.configure(builder)
            return builder.build()
        }
    }

    /**
     * Sets the accept language for the outgoing request headers for `operations`, `push`, and `inbox` objects.
     * The value can be further modified in each object individually.
     *
     ** Standard RFC "Accept-Language"**: [RFC 7231, Section 5.3.5](https://tools.ietf.org/html/rfc7231#section-5.3.5)
     *
     * Response texts are based on this setting. For example, when `"de"` is set, the server
     * will return operation texts in German (if available).
     *
     * @Param lang: The language code to set for the `Accept-Language` header.
     */
    fun setAcceptLanguage(lang: String) {
        acceptLanguage = lang
        operations.acceptLanguage = lang
        push.acceptLanguage = lang
        inbox.acceptLanguage = lang
        WMTLogger.i("Accept language set to $lang")
    }

    // creates operations service
    private fun createOperations(): OperationsService {
        WMTLogger.d("Creating OperationsService in WultraMobileToken")

        val operationService = OperationsService(
            powerAuthSDK,
            appContext,
            okHttpClient,
            powerAuthSDK.configuration.baseEndpointUrl,
            null,
            userAgent,
            gsonBuilder
        )
        operationService.acceptLanguage = acceptLanguage

        return operationService
    }

    // creates push service
    private fun createPush(): PushService {
        WMTLogger.d("Creating Push Service in WultraMobileToken")

        val pushService = PushService(
            powerAuthSDK,
            appContext,
            okHttpClient,
            powerAuthSDK.configuration.baseEndpointUrl,
            null,
            userAgent
        )
        pushService.acceptLanguage = acceptLanguage
        return pushService
    }

    // creates inbox service
    private fun createInbox(): InboxService {
        WMTLogger.d("Creating Inbox Service in WultraMobileToken")

        val inboxService = InboxService(
            powerAuthSDK,
            appContext,
            okHttpClient,
            powerAuthSDK.configuration.baseEndpointUrl,
            null,
            userAgent,
            gsonBuilder
        )
        inboxService.acceptLanguage = acceptLanguage
        return inboxService
    }

    // creates inbox service
    private fun createOidc(): OidcService {
        WMTLogger.d("Creating Inbox Service in WultraMobileToken")

        val oidcService = OidcService(
            powerAuthSDK,
            appContext,
            okHttpClient,
            powerAuthSDK.configuration.baseEndpointUrl,
            null,
            userAgent,
            gsonBuilder
        )
        oidcService.acceptLanguage = acceptLanguage
        return oidcService
    }
}
