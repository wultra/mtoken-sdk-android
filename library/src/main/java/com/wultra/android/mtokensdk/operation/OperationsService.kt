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

package com.wultra.android.mtokensdk.operation

import android.content.Context
import com.wultra.android.mtokensdk.api.IApiCallResponseListener
import com.wultra.android.mtokensdk.api.ResponseBodyConverter
import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.general.StatusResponse
import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.common.IPowerAuthTokenProvider
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.mtokensdk.common.SSLValidationStrategy
import com.wultra.android.mtokensdk.common.TokenManager
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient
import java.lang.Exception
import java.util.*

/**
 * Convenience factory method to create an IOperationsService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context object
 * @param baseURL Base URL where the operations endpoint rests.
 * @param httpClient OkHttpClient for API communication
 */
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, httpClient: OkHttpClient): IOperationsService {
    return OperationsService(this, appContext, httpClient, baseURL, TokenManager(appContext, this.tokenStore))
}

/**
 * Convenience factory method to create an IOperationsService instance
 * from given PowerAuthSDK instance.
 *
 * @param appContext Application Context object
 * @param baseURL Base URL where the operations endpoint rests.
 * @param strategy SSL validation strategy for networking
 */
fun PowerAuthSDK.createOperationsService(appContext: Context, baseURL: String, strategy: SSLValidationStrategy): IOperationsService {
    val builder = OkHttpClient.Builder()
    strategy.configure(builder)
    Logger.configure(builder)
    return createOperationsService(appContext, baseURL, builder.build())
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
    constructor(powerAuthSDK: PowerAuthSDK, appContext: Context, httpClient: OkHttpClient, baseURL: String, tokenProvider: IPowerAuthTokenProvider) {
        this.powerAuthSDK = powerAuthSDK
        this.appContext = appContext
        this.operationApi = OperationApi(httpClient, baseURL, appContext, tokenProvider, powerAuthSDK)
    }

    override fun isLoadingOperations() = operationsLoading

    override fun getLastOperationsResult() = lastOperationsResult

    override fun getOperations(listener: IGetOperationListener?) {
        getOperationsPrivate(null, listener)
    }

    override fun getOperations(customConverter: ResponseBodyConverter<OperationListResponse>, listener: IGetOperationListener?) {
        getOperationsPrivate(customConverter, listener)
    }

    private fun getOperationsPrivate(customConverter: ResponseBodyConverter<OperationListResponse>?, listener: IGetOperationListener?) {
        synchronized(mutex) {
            listener?.let { tasks.add(listener) }
            if (operationsLoading) {
                Logger.d("getOperation requested, but another request already running")
                return
            }
            operationsLoading = true
            updateOperationsListAsync(customConverter)
        }
    }

    override fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener) {
        val authorizeRequest = AuthorizeRequest(AuthorizeRequestObject(operation.id, operation.data))
        operationApi.authorize(authorizeRequest, authentication, object : IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                listener.onSuccess()
            }

            override fun onFailure(e: Throwable) {
                listener.onError(ApiError(e))
            }
        })
    }

    override fun rejectOperation(operation: IOperation, reason: RejectionReason, listener: IRejectOperationListener) {
        val rejectRequest = RejectRequest(RejectRequestObject(operation.id, reason.reason))
        operationApi.reject(rejectRequest, object : IApiCallResponseListener<StatusResponse> {
            override fun onSuccess(result: StatusResponse) {
                listener.onSuccess()
            }

            override fun onFailure(e: Throwable) {
                listener.onError(ApiError(e))
            }
        })
    }

    @Throws
    override fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication): String {
        return powerAuthSDK.offlineSignatureWithAuthentication(appContext, authentication, OperationApi.OFFLINE_AUTHORIZE_URL_ID, operation.dataForOfflineSigning(), operation.nonce)
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

    private fun updateOperationsListAsync(customConverter: ResponseBodyConverter<OperationListResponse>? = null) {
        operationApi.list(object : IApiCallResponseListener<OperationListResponse> {
            override fun onSuccess(result: OperationListResponse) {
                lastOperationsResult = SuccessOperationsResult(result.responseObject)
                synchronized(mutex) {
                    tasks.forEach { it.onSuccess(result.responseObject) }.also { tasks.clear() }
                    operationsLoading = false
                }
            }
            override fun onFailure(e: Throwable) {
                val error = ApiError(e)
                lastOperationsResult = ErrorOperationsResult(error)
                synchronized(mutex) {
                    tasks.forEach { it.onError(error) }.also { tasks.clear() }
                    operationsLoading = false
                }
            }
        }, customConverter)
    }

}