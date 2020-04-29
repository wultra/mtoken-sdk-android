/*
 * Copyright (c) 2018, Wultra s.r.o. (www.wultra.com).
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
import com.wultra.android.mtokensdk.api.general.GeneralFailure
import com.wultra.android.mtokensdk.api.operation.OperationApi
import com.wultra.android.mtokensdk.api.operation.model.*
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference

abstract class OperationsResult
data class SuccessOperationsResult(val operations: List<Operation>): OperationsResult()
data class ErrorOperationsResult(val error: GeneralFailure): OperationsResult()

fun PowerAuthSDK.createOperationsManager(appContext: Context): OperationsManager {
    val tm = TokenManager(appContext, tokenStore)
    val sm = SignatureManager(appContext, this)
    val api = OperationApi(OkHttpClient(), tm, sm)
    return OperationsManager(api, this, appContext)
}

/**
 * Manager for handling operations.
 *
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class OperationsManager constructor(private val operationApi: OperationApi, private val powerAuthSDK: PowerAuthSDK, private val appContext: Context) {

    interface IOperationsManagerListener {
        fun operationsChanged(operations: List<Operation>)
        fun operationsLoading(loading: Boolean)
        fun operationsFailed(error: GeneralFailure)
    }

    interface IGetOperationListener {
        fun onError(error: GeneralFailure)
        fun onSuccess(operations: List<Operation>)
    }

    var listener: WeakReference<IOperationsManagerListener>? = null

    var operationsLoading = false
        private set(value) {
            field = value
            listener?.get()?.operationsLoading(value)
        }

    private val tasks = mutableListOf<IGetOperationListener>()

    var lastOperationsResult: OperationsResult? = null
        private set(value) {
            field = value
            when (value) {
                is SuccessOperationsResult -> listener?.get()?.operationsChanged(value.operations)
                is ErrorOperationsResult -> listener?.get()?.operationsFailed(value.error)
            }
        }

    @Synchronized
    private suspend fun updateOperationsListSuspended() = withContext(Dispatchers.IO) {
        try {
            operationsLoading = true
            val operationsListResult = operationApi.list().await()
            // when there are no pending operations, we can make sure to clear all notifications
            if (operationsListResult.responseObject.isEmpty()) {
                // TODO: clear push
                //pushManager.get().clearAllStatusbarNotifications()
            }
            lastOperationsResult = SuccessOperationsResult(operationsListResult.responseObject)
            tasks.forEach { it.onSuccess(operationsListResult.responseObject) }.also { tasks.clear() }
        } catch (e: Exception) {
            val error = GeneralFailure(e)
            lastOperationsResult = ErrorOperationsResult(error)
            tasks.forEach { it.onError(error) }.also { tasks.clear() }
        } finally {
            operationsLoading = false
        }
    }

    /**
     * Launch fetching operations via actor.
     */
    @Synchronized
    fun launchFetchOperations(listener: IGetOperationListener? = null) {
        listener?.let { tasks.add(listener) }
        if (operationsLoading) {
            return
        }
        apiCoroutineScope.launch {
            updateOperationsListSuspended()
        }
    }

    interface IRejectOperationListener {
        fun onSuccess()
        fun onError(error: GeneralFailure)
    }

    @Synchronized
    fun rejectOperation(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener) {
        apiCoroutineScope.launch {
            rejectOperationSuspended(operation, reason, listener)
        }
    }

    @Synchronized
    private suspend fun rejectOperationSuspended(operation: Operation, reason: RejectionReason, listener: IRejectOperationListener) = withContext(Dispatchers.IO) {

        try {
            val rejectRequest = RejectRequest(RejectRequestObject(operation.id, reason.reason))
            operationApi.reject(rejectRequest).await()
            //when (operation.allowedSignatureType.type) {
                // TODO: stats
//                AllowedSignatureType.Type.MULTIFACTOR_1FA -> statsManager.record(StatsEvent.OP_1FA_REJECT)
//                AllowedSignatureType.Type.MULTIFACTOR_2FA -> statsManager.record(StatsEvent.OP_2FA_REJECT)
            //}
            listener.onSuccess()
        } catch (e: Exception) {
            listener.onError(GeneralFailure(e))
        }
    }

    interface IAcceptOperationListener {
        fun onSuccess()
        fun onError(error: GeneralFailure)
    }

    @Synchronized
    fun authorizeOperationWithPassword(operation: Operation, password: String, listener: IAcceptOperationListener) {
        apiCoroutineScope.launch {
            authorizeOperationSuspended(operation, password, null, listener)
        }
    }

    @Synchronized
    fun authorizeOperation(operation: Operation, biometry: ByteArray, listener: IAcceptOperationListener) {
        apiCoroutineScope.launch {
            authorizeOperationSuspended(operation, null, biometry, listener)
        }
    }

    @Synchronized
    private suspend fun authorizeOperationSuspended(operation: Operation, password: String?, biometry: ByteArray?, listener: IAcceptOperationListener) {
        try {
            val authorizeRequest = AuthorizeRequest(AuthorizeRequestObject(operation.id, operation.data))
            operationApi.authorize(authorizeRequest, operation.allowedSignatureType.type, password, biometry).await()
            // TODO: stats and action manager
            listener.onSuccess()
        } catch (e: Exception) {
            listener.onError(GeneralFailure(e))
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

    /*
     * Sign offline QR operation with password.
     */
    @Synchronized
    @WorkerThread
    fun signOfflineOperationWithBiometry(biometry: ByteArray, offlineOperation: QROperation): String? {
        return offlineSignature(null, biometry, offlineOperation)
    }

    /*
     * Sign offline QR operation with password.
     */
    @Synchronized
    @WorkerThread
    fun signOfflineOperationWithPassword(password: String, offlineOperation: QROperation): String? {
        return offlineSignature(password, null, offlineOperation)
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
}