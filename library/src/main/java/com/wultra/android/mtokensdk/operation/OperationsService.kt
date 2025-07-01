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
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.UserAgent
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException
import com.wultra.android.powerauth.networking.tokens.IPowerAuthTokenProvider
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*

private typealias GetOperationsCallback = (result: Result<List<UserOperation>>) -> Unit

/**
 * Service for operations handling.
 */
@Suppress("EXPERIMENTAL_API_USAGE", "ConvertSecondaryConstructorToPrimary")
class OperationsService {

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

    /**
     * Listener gets notified about changes in operations loading and its result.
     */
    var listener: IOperationsServiceListener? = null

    /**
     * Accept language for the outgoing requests headers.
     * Default value is "en".
     * Changing this value updates the accept language of the underlying operationsApi.
     *
     * Standard RFC "Accept-Language" https://tools.ietf.org/html/rfc7231#section-5.3.5
     * Response texts are based on this setting. For example when "de" is set, server
     * will return operation texts in german (if available).
     */
    var acceptLanguage: String
        get() = operationApi.acceptLanguage
        set(value) {
            operationApi.acceptLanguage = value
        }

    /**
     * A custom interceptor can intercept each service call.
     *
     * You can use this for request/response logging into your own log system.
     */
    var okHttpInterceptor: OkHttpBuilderInterceptor?
        get() = operationApi.okHttpInterceptor
        set(value) {
            operationApi.okHttpInterceptor = value
        }

    private val powerAuthSDK: PowerAuthSDK
    private val appContext: Context
    private var timer: Timer? = null
    private val minimumTimePollingInterval: Long = 5_000

    /**
     * Last result of getOperations. This value is not persistently cached.
     */
    val lastFetchResult: Result<List<UserOperation>>?
        get() = synchronized(mutex) { lastFetchOperationsResult }

    // Contains last fetched result with operations. Must be accessed from the mutex.
    private var lastFetchOperationsResult: Result<List<UserOperation>>? = null

    // Operation register holds operations in order
    private val operationsRegister: OperationsRegister by lazy {
        OperationsRegister { data ->
            listener?.operationsChanged(data.operationsList, data.removed, data.added)
        }
    }

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

    /**
     * If operations are loading.
     */
    fun isLoadingOperations() = synchronized(mutex) { tasks.isNotEmpty() }

    /**
     * Retrieves user operations.
     *
     * @param callback Callback with result
     */
    fun getOperations(callback: GetOperationsCallback) {
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
                WMTLogger.w("getOperation requested, but another request already running")
            }
        }
    }

    /**
     * Fetch operations from the server and report result to service's [IOperationsService.listener]. The function is effective
     * only if service's listener is set.
     */
    fun fetchOperations() = getOperations {}

    private fun processOperationsListResult(result: Result<List<UserOperation>>) {
        synchronized(mutex) {
            // At first, capture result to "lastFetchResult"
            lastFetchOperationsResult = result
            // Then, report result back to the listener, if it's set.
            listener?.let { listener ->
                result.onSuccess { operationsRegister.replace(it) }
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

    /**
     * Retrieves the history of user operations with its current status.
     *
     * @param authentication A multi-factor authentication object for signing. 2FA should be used (password or biometrics).
     * @param callback Callback with result.
     */
    fun getHistory(authentication: PowerAuthAuthentication, callback: (result: Result<List<UserOperation>>) -> Unit) {
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

    /**
     * Authorises operation with provided authentication
     *
     * @param operation Operation for approval
     * @param authentication Multi-factor authentication object for signing, which depends on the operation type but usually 2FA (password or biometrics)
     * @param callback Callback with result.
     */
    fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, callback: (result: Result<Unit>) -> Unit) {
        authorizeOperation(operation, authentication, null, callback)
    }

    /**
     * Authorises operation with provided authentication and mobile token data
     *
     * @param operation Operation for approval
     * @param authentication Multi-factor authentication object for signing, which depends on the operation type but usually 2FA (password or biometrics)
     * @param mobileTokenData Additional mobile token data (available with PowerAuth server 1.10+)
     * @param callback Callback with result.
     */
    fun authorizeOperation(operation: IOperation, authentication: PowerAuthAuthentication, mobileTokenData: String?, callback: (result: Result<Unit>) -> Unit) {

        val timeService = powerAuthSDK.timeSynchronizationService
        val currentDate = if (timeService.isTimeSynchronized) {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeService.currentTime), ZoneId.systemDefault())
        } else ZonedDateTime.now()

        val authorizeRequest = AuthorizeRequest(AuthorizeRequestObject(operation, currentDate, mobileTokenData))
        operationApi.authorize(
            authorizeRequest,
            authentication,
            object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    operationsRegister.remove(operation)
                    callback(Result.success(Unit))
                }

                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }
            }
        )
    }

    /**
     * Rejects operation with provided reason
     *
     * @param operation Operation to reject
     * @param reason Rejection reason
     * @param callback Callback with result.
     */
    fun rejectOperation(operation: IOperation, reason: RejectionData, callback: (result: Result<Unit>) -> Unit) {
        val rejectRequest = RejectRequest(RejectRequestObject(operation.id, reason.serialized))
        operationApi.reject(
            rejectRequest,
            object : IApiCallResponseListener<StatusResponse> {
                override fun onSuccess(result: StatusResponse) {
                    operationsRegister.remove(operation)
                    callback(Result.success(Unit))
                }

                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }
            }
        )
    }

    /**
     * Sign offline QR operation with provided authentication.
     *
     * @param operation Operation to approve
     * @param authentication Multi-factor authentication object for signing, which depends on the operation type but usually 2FA (password or biometrics)
     * @param uriId uriId: Custom signature URI ID of the operation. Use URI ID under which the operation was
     * created on the server. Default value is `/operation/authorize/offline`.
     *
     * @throws Exception Various exceptions, based on the error.
     *
     * @return Signature that should be displayed to the user
     */
    @Throws
    fun authorizeOfflineOperation(operation: QROperation, authentication: PowerAuthAuthentication, uriId: String = OperationApi.OFFLINE_AUTHORIZE_URI_ID): String {
        return powerAuthSDK.offlineSignatureWithAuthentication(appContext, authentication, uriId, operation.dataForOfflineSigning(), operation.nonce)
            ?: throw Exception("Cannot sign this operation")
    }

    /**
     * Retrieves operation detail based on operation ID
     *
     * @param operationId The identifier of the specific operation.
     * @param callback Callback with result.
     */
    fun getDetail(operationId: String, callback: (Result<UserOperation>) -> Unit) {
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

    /**
     * Claims the "non-personalized" operation and assigns it to the user.
     *
     * @param operationId Operation ID that will be claimed as belonging to the user.
     * @param callback Callback with result.
     */
    fun claim(operationId: String, callback: (Result<UserOperation>) -> Unit) {
        val claimRequest = OperationClaimDetailRequest(OperationClaimDetailData(operationId))
        operationApi.claim(
            claimRequest,
            object : IApiCallResponseListener<OperationClaimDetailResponse> {
                override fun onFailure(error: ApiError) {
                    callback(Result.failure(ApiErrorException(error)))
                }

                override fun onSuccess(result: OperationClaimDetailResponse) {
                    operationsRegister.add(result.responseObject)
                    callback(Result.success(result.responseObject))
                }
            }
        )
    }

    /**
     * Returns if operation polling is running
     */
    fun isPollingOperations() = timer != null

    /**
     * Starts polling operations from the server. You can observe the polling via [listener].
     *
     * If operations are already polling, this call is ignored
     * and the polling interval won't be changed.
     *
     * @param pollingInterval Polling interval in milliseconds, default value is 7s and minimum is 5s
     * @param delayStart When true, polling starts after the first [pollingInterval] passes
     *                   - By default it is set to false and polling starts immediately.
     */
    @Synchronized
    fun startPollingOperations(pollingInterval: Long = 7_000, delayStart: Boolean = false) {
        if (timer != null) {
            WMTLogger.w("Polling already in progress")
            return
        }

        val delay = if (delayStart) {
            pollingInterval
        } else {
            0
        }

        val adjustedInterval = if (pollingInterval < minimumTimePollingInterval) {
            WMTLogger.w("Operations polling interval: $pollingInterval, must not be set below $minimumTimePollingInterval to prevent server overload.")
            minimumTimePollingInterval
        } else {
            pollingInterval
        }

        val t = Timer("OperationsServiceTimer")
        t.schedule(
            object : TimerTask() {
                override fun run() {
                    fetchOperations()
                }
            },
            delay,
            adjustedInterval
        )
        timer = t
        WMTLogger.i("Polling started with $pollingInterval milliseconds interval")
    }

    /**
     * Stops operation polling
     */
    fun stopPollingOperations() {
        timer?.cancel()
        timer = null
        WMTLogger.i("Operation polling stopped")
    }
}

private data class CallbackData(val operationsList: List<UserOperation>, val removed: List<UserOperation>, val added: List<UserOperation>)

private class OperationsRegister(private val onChangeCallback: (CallbackData) -> Unit) {
    private val currentOperations = mutableListOf<UserOperation>()

    // Mutex to prevent race conditions from running multiple operations calls simultaneously
    private val currentOperationsMutex = Object()

    // Adds an operation to the register
    fun add(operation: UserOperation) {
        synchronized(currentOperationsMutex) {
            if (currentOperations.none { it.id == operation.id }) {
                currentOperations.add(operation)
                onChangeCallback(CallbackData(currentOperations, emptyList(), listOf(operation)))
            }
        }
    }

    // Adds a multiple operations to the register.
    // Returns list of added and removed operations.
    fun replace(operations: List<UserOperation>): Pair<List<UserOperation>, List<UserOperation>> {
        synchronized(currentOperationsMutex) {
            // Build a list of operations which were added
            val addedOperations = operations.filter { newOp ->
                currentOperations.none { it.id == newOp.id }
            }

            // Build a list of operations which were removed
            val removedOperations = currentOperations.filter { currentOp ->
                operations.none { it.id == currentOp.id }
            }

            // Remove operations which are no longer valid
            currentOperations.removeAll { op -> op.id in removedOperations.map { it.id } }

            // Append new operations
            currentOperations.addAll(addedOperations)

            // Notify about changes
            onChangeCallback(CallbackData(currentOperations, removedOperations, addedOperations))

            // Return added and removed operations
            return Pair(addedOperations, removedOperations)
        }
    }

    // Removes an operation from the register
    fun remove(operation: IOperation) {
        synchronized(currentOperationsMutex) {
            val operationToRemove = currentOperations.firstOrNull { it.id == operation.id }
            if (operationToRemove != null && currentOperations.removeAll { it.id == operationToRemove.id }) {
                onChangeCallback(CallbackData(currentOperations, listOf(operationToRemove), emptyList()))
            }
        }
    }
}
