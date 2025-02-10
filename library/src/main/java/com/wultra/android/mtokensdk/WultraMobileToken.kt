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
import com.wultra.android.mtokensdk.oidc.OIDCService
import com.wultra.android.mtokensdk.operation.OperationsService
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.mtokensdk.push.PushService
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Convenience factory method to create a WultraMobileToken instance
 * from given PowerAuthSDK instance from default values.
 *
 * @param appContext Application Context
 * @param okHttpClient HTTP client instance for networking
 * @param acceptLanguage The language code to set for the `Accept-Language` header.
 * @param tokenProvider PowerAuthToken provider. If null is provided, default internal implementation is provided.
 * @param userAgent Default user agent for each request.
 * @param gsonBuilder Custom GSON builder for deserialization of request. If you want to provide your own
 * deserialization logic, we recommend adding it to the instance obtained from the [OperationsUtils.defaultGsonBuilder].
 * @return WultraMobileToken instance
 */
fun PowerAuthSDK.createWultraMobileToken(
    appContext: Context,
    okHttpClient: OkHttpClient? = null,
    acceptLanguage: String? = null,
    tokeProvider: IPowerAuthTokenProvider? = null,
    userAgent: UserAgent? = null,
    gsonBuilder: GsonBuilder? = null
): WultraMobileToken {
    return WultraMobileToken(
        this,
        appContext,
        okHttpClient,
        acceptLanguage,
        tokeProvider,
        userAgent,
        gsonBuilder
    )
}

/**
 * WultraMobileToken is a centralized class responsible for initializing and providing access
 * to multiple SDK services. This class simplifies the setup and usage of the following components:
 *
 *  * @param powerAuthSDK PowerAuth instance
 * @param appContext Application Context
 * @param okHttpClient OkHttpClient for API communication
 * @param acceptLanguage The language code to set for the `Accept-Language` header.
 * @param tokenProvider PowerAuthToken provider. If null is provided, default internal implementation is provided.
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
    private val powerAuthSDK: PowerAuthSDK,
    private val appContext: Context,
    private val okHttpClient: OkHttpClient? = null,
    private var acceptLanguage: String? = null,
    private val tokenProvider: IPowerAuthTokenProvider? = null,
    private val userAgent: UserAgent? = null,
    private val gsonBuilder: GsonBuilder? = null
) {

    /**
     * Default OkHttpClient
     */
    companion object {
        private fun defaultOkHttpClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
            val strategy = SSLValidationStrategy.system()
            strategy.configure(builder)
            return builder.build()
        }
    }

    /** Lazy-loaded services backing fields */
    private val operationsBacking by lazy {
        Lazy {
            OperationsService(
                powerAuthSDK,
                appContext,
                okHttpClient ?: defaultOkHttpClient(),
                powerAuthSDK.configuration.baseEndpointUrl,
                tokenProvider,
                userAgent,
                gsonBuilder
            ).apply {
                this@WultraMobileToken.acceptLanguage?.let { acceptLanguage = it }
            }
        }
    }
    private val pushBacking by lazy {
        Lazy {
            PushService(
                powerAuthSDK,
                appContext,
                okHttpClient ?: defaultOkHttpClient(),
                powerAuthSDK.configuration.baseEndpointUrl,
                tokenProvider,
                userAgent
            ).apply {
                this@WultraMobileToken.acceptLanguage?.let { acceptLanguage = it }
            }
        }
    }
    private val inboxBacking by lazy {
        Lazy {
            InboxService(
                powerAuthSDK,
                appContext,
                okHttpClient ?: defaultOkHttpClient(),
                powerAuthSDK.configuration.baseEndpointUrl,
                tokenProvider,
                userAgent,
                gsonBuilder
            ).apply {
                this@WultraMobileToken.acceptLanguage?.let { acceptLanguage = it }
            }
        }
    }
    private val oidcBacking by lazy {
        Lazy {
            OIDCService(
                powerAuthSDK,
                appContext,
                okHttpClient ?: defaultOkHttpClient(),
                powerAuthSDK.configuration.baseEndpointUrl,
                tokenProvider,
                userAgent,
                gsonBuilder
            ).apply {
                this@WultraMobileToken.acceptLanguage?.let { acceptLanguage = it }
            }
        }
    }

    /**
     * Operations manager. Use for fetching pending lists, approving operations, etc.
     */
    val operations: OperationsService get() = operationsBacking.lazy

    /**
     * Push manager for registering the device to receive PowerAuth push notifications for a given PowerAuth activation.
     */
    val push: PushService get() = pushBacking.lazy

    /**
     * Inbox manager - receives messages to communicate with the user.
     */
    val inbox: InboxService get() = inboxBacking.lazy

    /**
     * OIDC manager - receive the config and help with OIDC activation preparation
     */
    val oidc: OIDCService get() = oidcBacking.lazy

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
        operationsBacking.optional?.acceptLanguage = lang
        pushBacking.optional?.acceptLanguage = lang
        inboxBacking.optional?.acceptLanguage = lang
        oidcBacking.optional?.acceptLanguage = lang
        WMTLogger.i("Accept language set to $lang")
    }
}
