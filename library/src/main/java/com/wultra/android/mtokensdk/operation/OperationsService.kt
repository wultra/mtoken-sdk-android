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
import com.wultra.android.mtokensdk.api.apiErrorForListener
import com.wultra.android.mtokensdk.api.operation.*
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.common.Logger
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.ssl.SSLValidationStrategy
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
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

private typealias GetOperationsCallback = (result: Result<List<UserOperation>>) -> Unit

@Suppress("EXPERIMENTAL_API_USAGE", "ConvertSecondaryConstructorToPrimary")
class OperationsService: IOperationsService {

    companion object {
        /**
         * Maximal duration in milliseconds of the request that can affect server time.
         * If request takes longer than this value, the value won't update server time
         */
        private const val SERVER_TIME_DELAY_THRESHOLD_MS = 1_000
        /**
         * Minimal delta change in server time to accept it as a change.
         */
        private const val MIN_SERVER_TIME_CHANGE_MS = 300
        /**
         * Delta change which is forced to be accepted even when the network conditions are not ideal
         */
        private const val FORCED_SERVER_TIME_CHANGE_MS = 20_000
    }

    override var listener: IOperationsServiceListener? = null

    override var acceptLanguage: String
        get() = operationApi.acceptLanguage
        set(value) {
            operationApi.acceptLanguage = value
        }

    override var okHttpInterceptor: OkHttpBuilderInterceptor?
        get() = operationApi.okHttpInterceptor
        set(value) {
            operationApi.okHttpInterceptor = value
        }

    private val powerAuthSDK: PowerAuthSDK
    private val appContext: Context
    private var timer: Timer? = null

    override val lastOperationsResult: Result<List<UserOperation>>?
        get() = synchronized(mutex) { lastGetOperationsResult }

    // Contains last fetched result with operations. Must be accessed from the mutex.
    private var lastGetOperationsResult: Result<List<UserOperation>>? = null

    // API class for communication.
    private val operationApi: OperationApi

    // List of tasks waiting for ongoing operation fetch to finish. If list is not empty, then
    // this indicate that operations loading is in progress.
    private val tasks = mutableListOf<GetOperationsCallback>()

    // Mutex
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

    private fun currentDate(): ZonedDateTime = run {
        val timeService = powerAuthSDK.timeSynchronizationService
        if (timeService.isTimeSynchronized) {
            val currentTimeInstant = Instant.ofEpochMilli(timeService.currentTime)
            val defaultTimeZoneId = ZoneId.systemDefault()
            return ZonedDateTime.ofInstant(currentTimeInstant, defaultTimeZoneId)
        } else {
            return ZonedDateTime.now()
        }
    }

    override fun isLoadingOperations() = synchronized(mutex) { tasks.isNotEmpty() }

    override fun getOperations(callback: GetOperationsCallback) {
        synchronized(mutex) {
            val startLoading = tasks.isEmpty()
            tasks.add(callback)
            if (startLoading) {
                // Notify start loading
                listener?.operationsLoading(true)
                operationApi.list(object : IApiCallResponseListener<OperationListResponse> {
                    override fun onSuccess(result: OperationListResponse) {
                        processOperationsListResult(Result.success(result.responseObject))
                    }
                    override fun onFailure(error: ApiError) {
                        processOperationsListResult(Result.failure(ApiErrorException(error)))
                    }
                })
            } else {
                Logger.d("getOperation requested, but another request already running")
            }
        }
    }

    private fun processOperationsListResult(result: Result<List<UserOperation>>) {
        synchronized(mutex) {
            // At first, capture result to "lastOperationsResult"
            lastGetOperationsResult = result
            // Then, report result back to the listener, if it's set.
            listener?.let { listener ->
                result.onSuccess { listener.operationsLoaded(it) }
                    .onFailure { listener.operationsFailed(it.apiErrorForListener()) }
            }
            // Now notify all tasks. We should iterate over copy of the list, to prevent
            // tasks modification in case that application start yet another update right from
            // the callback.
            val tasksCopy = tasks.toList()
            tasks.clear()
            // At this point, the loading is marked as finished.
            tasksCopy.forEach { it(result) }
            // And finally, report finish loading to the listener.
            listener?.operationsLoading(false)
        }
    }

    override fun getHistory(authentication: PowerAuthAuthentication, callback: (result: Result<List<OperationHistoryEntry>>) -> Unit) {
        operationApi.history(
            authentication,
            object : IApiCallResponseListener<OperationHistoryResponse> {
                override fun onSuccess(result: OperationHistoryResponse) {
                    callback(Result.success(result.responseObject))
                }

                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }
            }
        )
    }

    override fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, callback: (result: Result<Unit>) -> Unit) {

        val currentDate = currentDate()
        val authorizeRequest = AuthorizeRequest(AuthorizeRequestObject(operation, currentDate))
        operationApi.authorize(
            authorizeRequest,
            authentication,
            object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    callback(Result.success(Unit))
                }

                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }
            }
        )
    }

    override fun rejectOperation(operation: IOperation, reason: RejectionData, callback: (result: Result<Unit>) -> Unit) {
        val rejectRequest = RejectRequest(RejectRequestObject(operation.id, reason.serialized))
        operationApi.reject(
            rejectRequest,
            object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    callback(Result.success(Unit))
                }

                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }
            }
        )
    }

    @Throws
    override fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication, uriId: String): String {
        return powerAuthSDK.offlineSignatureWithAuthentication(appContext, authentication, uriId, operation.dataForOfflineSigning(), operation.nonce)
            ?: throw Exception("Cannot sign this operation")
    }

    override fun getDetail(operationId: String, callback: (Result<UserOperation>) -> Unit) {
        val detailRequest = OperationClaimDetailRequest(OperationClaimDetailData(operationId))

        operationApi.getDetail(
            detailRequest,
            object : IApiCallResponseListener<OperationClaimDetailResponse> {
                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }

                override fun onSuccess(result: OperationClaimDetailResponse) {
                    callback(Result.success(result.responseObject))
                }
            }
        )
    }

    override fun claim(operationId: String, callback: (Result<UserOperation>) -> Unit) {
        val claimRequest = OperationClaimDetailRequest(OperationClaimDetailData(operationId))
        operationApi.claim(
            claimRequest,
            object : IApiCallResponseListener<OperationClaimDetailResponse> {
                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }

                override fun onSuccess(result: OperationClaimDetailResponse) {
                    callback(Result.success(result.responseObject))
                }
            }
        )
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
        t.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    fetchOperations()
                }
            },
            delay,
            pollingInterval
        )
        timer = t
        Logger.d("Polling started with $pollingInterval milliseconds interval")
    }

    override fun stopPollingOperations() {
        timer?.cancel()
        timer = null
        Logger.d("Operation polling stopped")
    }
}
