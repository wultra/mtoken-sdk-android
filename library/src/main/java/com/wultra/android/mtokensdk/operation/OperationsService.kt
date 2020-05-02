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
import com.wultra.android.mtokensdk.api.general.ApiError
import com.wultra.android.mtokensdk.api.general.StatusResponse
import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.common.IPowerAuthTokenProvider
import com.wultra.android.mtokensdk.common.TokenManager
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
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

    private val mutex = Object()

    /**
     * Retrieves user operations and calls the listener when finished
     */
    fun getOperations(listener: IGetOperationListener? = null) {
        synchronized(mutex) {
            listener?.let { tasks.add(listener) }
            if (operationsLoading) {
                return
            }
            operationsLoading = true
            updateOperationsListAsync()
        }
    }


    fun authorizeOperation(operation: Operation, authentication: PowerAuthAuthentication, listener: IAcceptOperationListener) {
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

    fun rejectOperation(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener) {
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

    /**
     * Sign offline QR operation with password.
     */
    fun signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation): String? {
        return offlineSignature(null, biometry, offlineOperation)
    }

    /**
     * Sign offline QR operation with password.
     */
    fun signOfflineOperationWithPassword(password: String, offlineOperation: QROperation): String? {
        return offlineSignature(password, null, offlineOperation)
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
            override fun onFailure(e: Throwable) {
                val error = ApiError(e)
                lastOperationsResult = ErrorOperationsResult(error)
                synchronized(mutex) {
                    tasks.forEach { it.onError(error) }.also { tasks.clear() }
                    operationsLoading = false
                }
            }
        })
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