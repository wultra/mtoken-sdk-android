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
 * Convenience factory methods to create an WultraMobileToken instance
 * from given PowerAuthSDK instance where each service can be set individually.
 *
 * @param operationsConfig Application Context
 * @param pushConfig HTTP client instance for networking
 * @param inboxConfig Default user agent for each request.
 * @param acceptLanguage The language code to set for the `Accept-Language` header.
 * @param gsonBuilder Custom GSON builder for deserialization of request. If you want to provide your own
 * deserialization logic, we recommend adding it to the instance obtained from the [OperationsUtils].defaultGsonBuilder().
 * @return WultraMobileToken instance
 */
fun PowerAuthSDK.createWultraMobileToken(
    appContext: Context,
    operationsConfig: NetworkingConfig,
    pushConfig: NetworkingConfig? = null,
    inboxConfig: NetworkingConfig? = null,
    acceptLanguage: String = "en",
    gsonBuilder: GsonBuilder? = null
): WultraMobileToken {
    return WultraMobileToken(
        this,
        operationsConfig,
        pushConfig,
        inboxConfig,
        acceptLanguage,
        gsonBuilder
    )
}

/**
 * WultraMobileToken is a centralized class responsible for initializing and providing access
 * to multiple SDK services. This class simplifies the setup and usage of the following components:
 *
 * - [OperationsService]: Handles operations and workflows.
 * - [PushService]: Manages push notifications and related tasks.
 * - [InboxService]: Provides functionality for accessing and managing inbox messages.
 *
 * - **Extensible Configuration**: Supports custom `NetworkingConfig` for each service, enabling tailored setups.
 */

class WultraMobileToken private constructor(
    private val appContext: Context,
    private val powerAuthSDK: PowerAuthSDK,
    private val baseURL: String,
    private val okHttpClient: OkHttpClient,
    private var acceptLanguage: String,
    private val userAgent: UserAgent,
    private val gsonBuilder: GsonBuilder?,
    private val operationsConfig: NetworkingConfig?,
    private val pushConfig: NetworkingConfig?,
    private val inboxConfig: NetworkingConfig?
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
     * Default constructor for minimal initialization
     *
     * @param appContext Application Context
     * @param powerAuthSDK PowerAuth instance
     * @param okHttpClient OkHttpClient for API communication
     * @param acceptLanguage The language code to set for the `Accept-Language` header.
     * @param userAgent Default user agent used for each request.
     * @param gsonBuilder GSON builder for deserialization of request.
     */
    constructor(
        appContext: Context,
        powerAuthSDK: PowerAuthSDK,
        okHttpClient: OkHttpClient = defaultOkHttpClient(),
        acceptLanguage: String = "en",
        userAgent: UserAgent = UserAgent.libraryDefault(appContext),
        gsonBuilder: GsonBuilder? = null
    ) : this(
        appContext,
        powerAuthSDK,
        powerAuthSDK.configuration.baseEndpointUrl,
        okHttpClient,
        acceptLanguage,
        userAgent,
        gsonBuilder,
        null,
        null,
        null
    )

    /**
     * Constructor that allows networking for individual services (operations, push, and inbox)
     * to be configured separately.
     *
     * @param powerAuthSDK PowerAuth instance
     * @param operationsConfig Configuration for the OperationsService, including base URL, HTTP client, and other settings.
     * @param pushConfig (Optional) Configuration for the PushService. If not provided, defaults to `operationsConfig`.
     * @param inboxConfig (Optional) Configuration for the InboxService. If not provided, defaults to `operationsConfig`.
     * @param acceptLanguage The language code to set for the `Accept-Language` header (default is `"en"`).
     * @param gsonBuilder Custom GSON builder for request/response serialization. If null, defaults to the library's configuration.
     */
    constructor(
        powerAuthSDK: PowerAuthSDK,
        operationsConfig: NetworkingConfig,
        pushConfig: NetworkingConfig? = null,
        inboxConfig: NetworkingConfig? = null,
        acceptLanguage: String = "en",
        gsonBuilder: GsonBuilder? = null
    ) : this(
        operationsConfig.appContext,
        powerAuthSDK,
        operationsConfig.baseUrl ?: powerAuthSDK.configuration.baseEndpointUrl,
        operationsConfig.okHttpClient,
        acceptLanguage,
        operationsConfig.userAgent,
        gsonBuilder,
        operationsConfig,
        pushConfig ?: operationsConfig,
        inboxConfig ?: operationsConfig
    )

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
        WMTLogger.d("creatingOperations = ${operationsConfig?.baseUrl ?: baseURL}")

        val operationService = OperationsService(
            powerAuthSDK,
            operationsConfig?.appContext ?: appContext,
            operationsConfig?.okHttpClient ?: okHttpClient,
            operationsConfig?.baseUrl ?: baseURL,
            operationsConfig?.tokenProvider,
            operationsConfig?.userAgent
        )
        operationService.acceptLanguage = acceptLanguage

        return operationService
    }

    private fun createPush(): PushService {
        WMTLogger.d("creatingPush = ${pushConfig ?: baseURL}")

        val pushService = PushService(
            operationsConfig?.okHttpClient ?: okHttpClient,
            operationsConfig?.baseUrl ?: baseURL,
            powerAuthSDK,
            pushConfig?.appContext ?: appContext,
            pushConfig?.tokenProvider,
            pushConfig?.userAgent ?: userAgent
        )
        pushService.acceptLanguage = acceptLanguage
        return pushService
    }

    private fun createInbox(): InboxService {
        WMTLogger.d("creatingInbox = ${inboxConfig?.baseUrl ?: baseURL}")

        val inboxService = InboxService(
            inboxConfig?.okHttpClient ?: okHttpClient,
            inboxConfig?.baseUrl ?: powerAuthSDK.configuration.baseEndpointUrl,
            powerAuthSDK,
            inboxConfig?.appContext ?: appContext,
            inboxConfig?.tokenProvider,
            inboxConfig?.userAgent ?: userAgent,
            gsonBuilder
        )
        inboxService.acceptLanguage = acceptLanguage
        return inboxService
    }
}

/**
 * A configuration class for setting up networking parameters for SDK services.
 * This class encapsulates all the necessary components and parameters required for
 * API communication and service initialization.
 *
 * @property appContext The application context used for initializing services.
 * @property okHttpClient The `OkHttpClient` instance for making API requests.
 *                        Defaults to a client created by `WultraMobileToken.defaultOkHttpClientBuilder()`.
 * @property baseUrl (Optional) The base URL for API requests. If null, defaults to the `PowerAuthSDK` configuration.
 * @property tokenProvider (Optional) The token provider used for obtaining secure tokens for API requests.
 * @property userAgent The user agent string used in HTTP requests. Defaults to the library's standard user agent.
 *
 */
data class NetworkingConfig(
    val appContext: Context,
    val baseUrl: String? = null,
    val okHttpClient: OkHttpClient = WultraMobileToken.defaultOkHttpClient(),
    val tokenProvider: IPowerAuthTokenProvider? = null,
    val userAgent: UserAgent = UserAgent.libraryDefault(appContext)
)
