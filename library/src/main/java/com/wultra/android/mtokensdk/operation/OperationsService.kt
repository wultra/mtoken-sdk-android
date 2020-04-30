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
import android.support.annotation.WorkerThread
import com.wultra.android.mtokensdk.api.apiCoroutineScope
import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.common.IPowerAuthTokenProvider
import com.wultra.android.mtokensdk.common.TokenManager
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference

abstract class OperationsResult
data class SuccessOperationsResult(val operations: List<Operation>): OperationsResult()
data class ErrorOperationsResult(val error: ApiError): OperationsResult()

fun PowerAuthSDK.createOperationsService(
        appContext: Context,
        httpClient: OkHttpClient,
        baseURL: String,
        tokenProvider: IPowerAuthTokenProvider = TokenManager(appContext, this.tokenStore)): OperationsService {
    return OperationsService(this, appContext, tokenProvider, httpClient, baseURL)
}

/**
 * Manager for handling operations.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class OperationsService(private val powerAuthSDK: PowerAuthSDK,
                        private val appContext: Context,
                        tokenManager: IPowerAuthTokenProvider,
                        httpClient: OkHttpClient,
                        baseURL: String) {

    /**
     * Listener gets notified about changes in operations loading.
     */
    var listener: WeakReference<IOperationsManagerListener>? = null

    /**
     * If operation loading is currently in progress.
     */
    var operationsLoading = false
        private set(value) {
            field = value
            listener?.get()?.operationsLoading(value)
        }

    /**
     * Last cached operation result for easy access.
     */
    var lastOperationsResult: OperationsResult? = null
        private set(value) {
            field = value
            when (value) {
                is SuccessOperationsResult -> listener?.get()?.operationsChanged(value.operations)
                is ErrorOperationsResult -> listener?.get()?.operationsFailed(value.error)
            }
        }

    // API class for communication.
    private val operationApi: OperationApi = OperationApi(httpClient, baseURL, appContext, tokenManager, powerAuthSDK)

    // List of tasks waiting for ongoing operation fetch to finish
    private val tasks = mutableListOf<IGetOperationListener>()

    /**
     * Retrieves user operations and calls the listener when finished
     */
    @Synchronized
    fun getOperations(listener: IGetOperationListener? = null) {
        listener?.let { tasks.add(listener) }
        if (operationsLoading) {
            return
        }
        apiCoroutineScope.launch {
            updateOperationsListSuspended()
        }
    }

    @Synchronized
    fun authorizeOperation(operation: Operation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener) {
        apiCoroutineScope.launch {
            authorizeOperationSuspended(operation, authentication, listener)
        }
    }

    @Synchronized
    fun rejectOperation(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener) {
        apiCoroutineScope.launch {
            rejectOperationSuspended(operation, reason, listener)
        }
    }

    /**
     * Sign offline QR operation with password.
     */
    @Synchronized
    @WorkerThread
    fun signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation): String? {
        return offlineSignature(null, biometry, offlineOperation)
    }

    /**
     * Sign offline QR operation with password.
     */
    @Synchronized
    @WorkerThread
    fun signOfflineOperationWithPassword(password: String, offlineOperation: QROperation): String? {
        return offlineSignature(password, null, offlineOperation)
    }

    @Synchronized
    private suspend fun updateOperationsListSuspended() = withContext(Dispatchers.IO) {
        try {
            operationsLoading = true
            val operationsListResult = operationApi.list().await()
            lastOperationsResult = SuccessOperationsResult(operationsListResult.responseObject)
            tasks.forEach { it.onSuccess(operationsListResult.responseObject) }.also { tasks.clear() }
        } catch (e: Exception) {
            val error = ApiError(e)
            lastOperationsResult = ErrorOperationsResult(error)
            tasks.forEach { it.onError(error) }.also { tasks.clear() }
        } finally {
            operationsLoading = false
        }
    }

    @Synchronized
    private suspend fun rejectOperationSuspended(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener) = withContext(Dispatchers.IO) {
        try {
            val rejectRequest = RejectRequest(RejectRequestObject(operation.id, reason.reason))
            operationApi.reject(rejectRequest).await()
            listener.onSuccess()
        } catch (e: Exception) {
            listener.onError(ApiError(e))
        }
    }

    @Synchronized
    private suspend fun authorizeOperationSuspended(operation: Operation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener) {
        try {
            val authorizeRequest = AuthorizeRequest(AuthorizeRequestObject(operation.id, operation.data))
            operationApi.authorize(authorizeRequest, operation.allowedSignatureType.type, authentication).await()
            // TODO: stats and action manager
            listener.onSuccess()
        } catch (e: Exception) {
            listener.onError(ApiError(e))
        }
    }

    /**
     * Process loaded payload from a scanned offline QR.
     */
    fun processOfflineQrPayload(payload: String): QROperation? {
        try {
            val unverifiedOfflineOperation = QROperationParser.parse(payload)
            val verified = powerAuthSDK.verifyServerSignedData(unverifiedOfflineOperation.signedData,
                    unverifiedOfflineOperation.signature.signature,
                    unverifiedOfflineOperation.signature.isMaster())
            if (!verified) {
                throw IllegalArgumentException("Invalid offline operation")
            }
            return unverifiedOfflineOperation
        } catch (e: Exception) {
            return null
        }
    }

    private fun offlineSignature(password: String?, biometry: ByteArray?, offlineOperation: QROperation): String? {
        if (password == null && biometry == null) {
            throw IllegalArgumentException("Password or biometry needs to be set")
        }
        val authentication = PowerAuthAuthentication()
        authentication.usePossession = true
        authentication.usePassword = password
        authentication.useBiometry = biometry
        // TODO: stats!
        return powerAuthSDK.offlineSignatureWithAuthentication(appContext, authentication, OperationApi.OFFLINE_AUTHORIZE_URL_ID, offlineOperation.dataForOfflineSigning(), offlineOperation.nonce)
    }

    interface IAcceptOperationListener {
        fun onSuccess()
        fun onError(error: ApiError)
    }

    interface IRejectOperationListener {
        fun onSuccess()
        fun onError(error: ApiError)
    }

    interface IOperationsManagerListener {
        fun operationsChanged(operations: List<Operation>)
        fun operationsLoading(loading: Boolean)
        fun operationsFailed(error: ApiError)
    }

    interface IGetOperationListener {
        fun onError(error: ApiError)
        fun onSuccess(operations: List<Operation>)
    }
}