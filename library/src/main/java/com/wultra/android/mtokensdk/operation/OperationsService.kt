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

package com.wultra.android.mtokensdk.operation

import android.content.Context
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.operation.*
import com.wultra.android.mtokensdk.api.operation.AuthorizeRequest
import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.RejectRequest
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient
import java.util.*

/**
 * Convenience factory method to create an IOperationsService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context object.
 * @param baseURL Base URL where the operations endpoint rests.
 * @param httpClient OkHttpClient for API communication.
 * @param userAgent Default user agent for each request.
 * @param gsonBuilder Custom GSON builder for deserialization of request. If you want to provide or own
 * deserialization logic, we recommend adding to the instance obtained from the OperationsUtils.defaultGsonBuilder().
 */
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, httpClient: OkHttpClient, userAgent: UserAgent? = null, gsonBuilder: GsonBuilder? = null): IOperationsService {
    return OperationsService(this, appContext, httpClient, baseURL, null, userAgent, gsonBuilder)
}

/**
 * Convenience factory method to create an IOperationsService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context object.
 * @param baseURL Base URL where the operations endpoint rests.
 * @param strategy SSL validation strategy for networking.
 * @param userAgent Default user agent for each request.
 * @param gsonBuilder Custom GSON builder for deserialization of request. If you want to provide or own
 * deserialization logic, we recommend adding to the instance obtained from the OperationsUtils.defaultGsonBuilder().
 */
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy, userAgent: UserAgent? = null, gsonBuilder: GsonBuilder? = null): IOperationsService {
    val builder = OkHttpClient.Builder()
    strategy.configure(builder)
    Logger.configure(builder)
    return createOperationsService(appContext, baseURL, builder.build(), userAgent, gsonBuilder)
}

@Suppress("EXPERIMENTAL_API_USAGE", "ConvertSecondaryConstructorToPrimary")
class OperationsService: IOperationsService {

    override var listener: IOperationsServiceListener? = null

    override var acceptLanguage: String
        get() = operationApi.acceptLanguage
        set(value) {
            operationApi.acceptLanguage = value
        }

    private val powerAuthSDK: PowerAuthSDK
    private val appContext: Context
    private var timer: Timer? = null

    private var operationsLoading = false
        set(value) {
            field = value
            listener?.operationsLoading(value)
        }

    private var lastOperationsResult: OperationsResult? = null
        set(value) {
            field = value
            when (value) {
                is SuccessOperationsResult -> listener?.operationsLoaded(value.operations)
                is ErrorOperationsResult -> listener?.operationsFailed(value.error)
            }
        }

    // API class for communication.
    private val operationApi: OperationApi

    // List of tasks waiting for ongoing operation fetch to finish
    private val tasks = mutableListOf<IGetOperationListener>()

    // IsLoading mutex
    private val mutex = Object()

    /**
     * Constructs OperationService
     *
     * @param powerAuthSDK PowerAuth instance
     * @param appContext Application Context object
     * @param httpClient OkHttpClient for API communication
     * @param baseURL Base URL where the operations endpoint rests.
     * @param tokenProvider PowerAuthToken provider. If null is provided, default internal implementation is provided.
     *
     */
    constructor(powerAuthSDK: PowerAuthSDK, appContext: Context, httpClient: OkHttpClient, baseURL: String, tokenProvider: IPowerAuthTokenProvider? = null, userAgent: UserAgent? = null, gsonBuilder: GsonBuilder? = null) {
        this.powerAuthSDK = powerAuthSDK
        this.appContext = appContext
        this.operationApi = OperationApi(httpClient, baseURL, appContext, powerAuthSDK, tokenProvider, userAgent, gsonBuilder)
    }

    override fun isLoadingOperations() = operationsLoading

    override fun getLastOperationsResult() = lastOperationsResult

    override fun getOperations(listener: IGetOperationListener?) {
        synchronized(mutex) {
            listener?.let { tasks.add(listener) }
            if (operationsLoading) {
                Logger.d("getOperation requested, but another request already running")
                return
            }
            operationsLoading = true
            updateOperationsListAsync()
        }
    }

    override fun getHistory(authentication: PowerAuthAuthentication, listener: IGetHistoryListener) {
        operationApi.history(authentication, object : IApiCallResponseListener<OperationHistoryResponse> {
            override fun onSuccess(result: OperationHistoryResponse) {
                listener.onSuccess(result.responseObject)
            }

            override fun onFailure(error: ApiError) {
                listener.onError(error)
            }
        })
    }

    override fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener) {
        val authorizeRequest = AuthorizeRequest(AuthorizeRequestObject(operation.id, operation.data))
        operationApi.authorize(authorizeRequest, authentication, object : IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                listener.onSuccess()
            }

            override fun onFailure(error: ApiError) {
                listener.onError(error)
            }
        })
    }

    override fun rejectOperation(operation: IOperation, reason: RejectionReason, listener: IRejectOperationListener) {
        val rejectRequest = RejectRequest(RejectRequestObject(operation.id, reason.reason))
        operationApi.reject(rejectRequest, object : IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                listener.onSuccess()
            }

            override fun onFailure(error: ApiError) {
                listener.onError(error)
            }
        })
    }

    @Throws
    override fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication, uriId: String): String {
        return powerAuthSDK.offlineSignatureWithAuthentication(appContext, authentication, uriId, operation.dataForOfflineSigning(), operation.nonce)
                ?: throw Exception("Cannot sign this operation")
    }

    override fun isPollingOperations() = timer != null

    @Synchronized
    override fun startPollingOperations(pollingInterval: Long, delayStart: Boolean) {
        if (timer != null) {
            Logger.w("Polling already in progress")
            return
        }

        val delay = if (delayStart) {
            pollingInterval
        } else {
            0
        }
        val t = Timer("OperationsServiceTimer")
        t.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                getOperations(null)
            }
        }, delay, pollingInterval)
        timer = t
        Logger.d("Polling started with $pollingInterval milliseconds interval")
    }

    override fun stopPollingOperations() {
        timer?.cancel()
        timer = null
        Logger.d("Operation polling stopped")
    }

    private fun updateOperationsListAsync() {
        operationApi.list(object : IApiCallResponseListener<OperationListResponse> {
            override fun onSuccess(result: OperationListResponse) {
                lastOperationsResult = SuccessOperationsResult(result.responseObject)
                synchronized(mutex) {
                    tasks.forEach { it.onSuccess(result.responseObject) }.also { tasks.clear() }
                    operationsLoading = false
                }
            }
            override fun onFailure(error: ApiError) {
                lastOperationsResult = ErrorOperationsResult(error)
                synchronized(mutex) {
                    tasks.forEach { it.onError(error) }.also { tasks.clear() }
                    operationsLoading = false
                }
            }
        })
    }

}