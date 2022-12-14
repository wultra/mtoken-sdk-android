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

package com.wultra.android.mtokensdk.test

import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntry
import com.wultra.android.mtokensdk.api.operation.model.OperationHistoryEntryStatus
import com.wultra.android.mtokensdk.api.operation.model.QROperationParser
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.operation.*
import com.wultra.android.powerauth.networking.error.ApiError
import io.getlime.security.powerauth.networking.response.IActivationRemoveListener
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.*
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Integration tests are calling a real backend server (based on configuration inside the "${ROOT_FOLDER}/configs/integration-tests.properties" file).
 */
class IntegrationTests {

    companion object {

        lateinit var ops: IOperationsService
        private lateinit var pa: PowerAuthSDK
        const val pin = "1234"

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                val result = IntegrationUtils.prepareActivation(pin)
                pa = result.first
                ops = result.second
            } catch (e: Throwable) {
                Assert.fail("Activation preparation failed: $e")
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            IntegrationUtils.removeRegistration(pa.activationIdentifier)
            pa.removeActivationLocal(IntegrationUtils.context)
        }
    }

    @Test
    fun testList() {
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val oplist = future.get(20, TimeUnit.SECONDS)
        Assert.assertNotNull(oplist)
    }

    // 1FA test are temporalily disabled

//    @Test
//    fun testApproveLogin() {
//        IntegrationUtils.createOperation(true)
//        val future = CompletableFuture<List<UserOperation>>()
//        ops.getOperations(object : IGetOperationListener {
//            override fun onSuccess(operations: List<UserOperation>) {
//                future.complete(operations)
//            }
//            override fun onError(error: ApiError) {
//                future.completeExceptionally(error.e)
//            }
//        })
//        val operations = future.get(20, TimeUnit.SECONDS)
//        Assert.assertTrue("Missing operation", operations.count() == 1)
//        val auth = PowerAuthAuthentication()
//        auth.usePossession = true
//        val opFuture = CompletableFuture<Any?>()
//        ops.authorizeOperation(operations.first(), auth, object : IAcceptOperationListener {
//            override fun onSuccess() {
//                opFuture.complete(null)
//            }
//            override fun onError(error: ApiError) {
//                opFuture.completeExceptionally(error.e)
//            }
//        })
//        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
//    }
//
//    @Test
//    fun testRejectLogin() {
//        IntegrationUtils.createOperation(true)
//        val future = CompletableFuture<List<UserOperation>>()
//        ops.getOperations(object : IGetOperationListener {
//            override fun onSuccess(operations: List<UserOperation>) {
//                future.complete(operations)
//            }
//            override fun onError(error: ApiError) {
//                future.completeExceptionally(error.e)
//            }
//        })
//        val operations = future.get(20, TimeUnit.SECONDS)
//        Assert.assertTrue("Missing operation", operations.count() == 1)
//        val opFuture = CompletableFuture<Any?>()
//        ops.rejectOperation(operations.first(), RejectionReason.UNEXPECTED_OPERATION, object : IRejectOperationListener {
//            override fun onSuccess() {
//                opFuture.complete(null)
//            }
//            override fun onError(error: ApiError) {
//                opFuture.completeExceptionally(error.e)
//            }
//        })
//        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
//    }

    @Test
    fun testApprovePayment() {
        IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Missing operation", operations.count() == 1)

        var auth = PowerAuthAuthentication.possessionWithPassword("xxxx") // wrong password on purpose
        val opFuture = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth, object : IAcceptOperationListener {
            override fun onSuccess() {
                opFuture.completeExceptionally(Exception("Operation should not be authorized"))
            }
            override fun onError(error: ApiError) {
                opFuture.complete(null)
            }
        })
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))

        auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val opFuture2 = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth, object : IAcceptOperationListener {
            override fun onSuccess() {
                opFuture2.complete(null)
            }
            override fun onError(error: ApiError) {
                opFuture2.completeExceptionally(error.e)
            }
        })
        Assert.assertNull(opFuture2.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testRejectPayment() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations(object : IGetOperationListener {
            override fun onSuccess(operations: List<UserOperation>) {
                future.complete(operations)
            }
            override fun onError(error: ApiError) {
                future.completeExceptionally(error.e)
            }
        })
        val operations = future.get(20, TimeUnit.SECONDS)
        val opFromList = operations.firstOrNull { it.id == op.operationId }
        if (opFromList == null) {
            Assert.fail("Operation was not in the list")
            return
        }
        val opFuture = CompletableFuture<Any?>()
        ops.rejectOperation(opFromList, RejectionReason.UNEXPECTED_OPERATION, object : IRejectOperationListener {
            override fun onSuccess() {
                opFuture.complete(null)
            }
            override fun onError(error: ApiError) {
                opFuture.completeExceptionally(error.e)
            }
        })
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testOperationPolling() {
        Assert.assertFalse(ops.isPollingOperations())
        var loadingCount  = 0
        val future = CompletableFuture<Any?>()
        ops.listener = object : IOperationsServiceListener {
            override fun operationsLoading(loading: Boolean) {
                if (loading) {
                    loadingCount += 1
                    if (loadingCount == 4) {
                        ops.stopPollingOperations()
                        future.complete(null)
                    }
                }
            }
            override fun operationsLoaded(operations: List<UserOperation>) {

            }
            override fun operationsFailed(error: ApiError) {

            }
        }
        ops.startPollingOperations(1_000, true)
        Assert.assertNull(future.get(20, TimeUnit.SECONDS))
        ops.stopPollingOperations()
        Assert.assertFalse(ops.isPollingOperations())
    }

    @Test
    fun testOperationHistory() {
        // lets create 1 operation and leave it in the state of "pending"
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val future = CompletableFuture<List<OperationHistoryEntry>?>()
        ops.getHistory(auth, object : IGetHistoryListener {
            override fun onSuccess(operations: List<OperationHistoryEntry>) {
                future.complete(operations)
            }

            override fun onError(error: ApiError) {
                future.complete(null)
            }
        })

        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertNotNull("Operations not retrieved" ,operations)
        if (operations == null) {
            return
        }

        val opRecord = operations.firstOrNull { it.operation.id == op.operationId }
        Assert.assertNotNull(opRecord)
        Assert.assertTrue(opRecord?.status == OperationHistoryEntryStatus.PENDING)
    }

    @Test
    fun testQROperation() {
        // create regular operation
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)

        // get QR data of the operation
        val qrData = IntegrationUtils.getQROperation(op)

        // parse the data
        val qrOperation = QROperationParser.parse(qrData.operationQrCodeData)

        // get the OTP with the "offline" signing
        val auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val otp = ops.authorizeOfflineOperation(qrOperation, auth)

        // verify the operation on the backend with the OTP
        val verifiedResult = IntegrationUtils.verifyQROperation(op, qrData, otp)

        Assert.assertTrue(verifiedResult.otpValid)
    }
}