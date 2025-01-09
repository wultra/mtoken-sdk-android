/*
 * Copyright (c) 2025, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.inbox.InboxService
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.operation.OperationsService
import com.wultra.android.mtokensdk.operation.OperationsUtils
import com.wultra.android.mtokensdk.push.PushService
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient

/**
 * Convenience factory method to create an WultraMobileToken instance
 * from given PowerAuthSDK instance from default values.
 *
 * @param appContext Application Context
 * @param okHttpClient HTTP client instance for networking
 * @param acceptLanguage The language code to set for the `Accept-Language` header.
 * @param userAgent Default user agent for each request.
 * @param gsonBuilder Custom GSON builder for deserialization of request. If you want to provide your own
 * deserialization logic, we recommend adding it to the instance obtained from the [OperationsUtils].defaultGsonBuilder().
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
    val operationsService: OperationsService by lazy { createOperations() }

    /**
     * Lazily initialized service for push notification registering
     */
    val pushService: PushService by lazy { createPush() }

    /**
     * Lazily initialized service for managing user's inbox
     */
    val inboxService: InboxService by lazy { createInbox() }

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
        operationsService.acceptLanguage = lang
        pushService.acceptLanguage = lang
        inboxService.acceptLanguage = lang
        WMTLogger.i("Accept language set to $lang")
    }

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

    private fun createPush(): PushService {
        WMTLogger.d("Creating Push Service in WultraMobileToken")

        val pushService = PushService(
            okHttpClient,
            powerAuthSDK.configuration.baseEndpointUrl,
            powerAuthSDK,
            appContext,
            null,
            userAgent
        )
        pushService.acceptLanguage = acceptLanguage
        return pushService
    }

    private fun createInbox(): InboxService {
        WMTLogger.d("Creating Inbox Service in WultraMobileToken")

        val inboxService = InboxService(
            okHttpClient,
            powerAuthSDK.configuration.baseEndpointUrl,
            powerAuthSDK,
            appContext,
            null,
            userAgent,
            gsonBuilder
        )
        inboxService.acceptLanguage = acceptLanguage
        return inboxService
    }
}
