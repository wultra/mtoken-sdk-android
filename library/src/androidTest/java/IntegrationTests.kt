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

import com.wultra.android.mtokensdk.api.operation.model.PreApprovalScreen
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheck
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheckType
import com.wultra.android.mtokensdk.api.operation.model.QROperationParser
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.api.operation.model.UserOperationStatus
import com.wultra.android.mtokensdk.operation.*
import com.wultra.android.mtokensdk.operation.RejectionData
import com.wultra.android.powerauth.networking.error.ApiError
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Integration tests are calling a real backend server (based on configuration inside the "${ROOT_FOLDER}/configs/integration-tests.properties" file).
 */
class IntegrationTests {

    private lateinit var ops: IOperationsService
    private lateinit var pa: PowerAuthSDK
    private val pin = "1234"

    @Before
    fun setup() {
        try {
            val result = IntegrationUtils.prepareActivation(pin)
            pa = result.first
            ops = result.second
        } catch (e: Throwable) {
            Assert.fail("Activation preparation failed: $e")
        }
    }

    @After
    fun tearDown() {
        if (!::pa.isInitialized) {
            return
        }
        IntegrationUtils.removeRegistration(pa.activationIdentifier)
        pa.removeActivationLocal(IntegrationUtils.context)
    }

    init {
        initThreeTen()
    }

    @Test
    fun testList() {
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations { result ->
            result
                .onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }
        val oplist = future.get(20, TimeUnit.SECONDS)
        Assert.assertNotNull(oplist)
    }

    /** currentServerDate was removed in favour of PowerAuthSDK timeSynchronizationService */
    @Test
    fun testServerTime() {
        var currentTime: ZonedDateTime? = null

        val timeService = pa.timeSynchronizationService
        if (timeService.isTimeSynchronized) {
            val instant = Instant.ofEpochMilli(timeService.currentTime)
            val zoneId = ZoneId.systemDefault()
            currentTime = ZonedDateTime.ofInstant(instant, zoneId)
        }
        Assert.assertNotNull(currentTime)
    }

    // 1FA test are temporally disabled

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

    @Test
    fun testApprovePayment() {
        IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }
        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Missing operation", operations.count() == 1)

        var auth = PowerAuthAuthentication.possessionWithPassword("xxxx") // wrong password on purpose
        val opFuture = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth) { result ->
            result.onSuccess { opFuture.completeExceptionally(Exception("Operation should not be authorized")) }
                .onFailure { opFuture.complete(null) }
        }
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))

        auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val opFuture2 = CompletableFuture<Any?>()
        ops.authorizeOperation(operations.first(), auth) { result ->
            result.onSuccess { opFuture2.complete(null) }
                .onFailure { opFuture2.completeExceptionally(it) }
        }
        Assert.assertNull(opFuture2.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testRejectPayment() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<List<UserOperation>>()
        ops.getOperations { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }
        val operations = future.get(20, TimeUnit.SECONDS)
        val opFromList = operations.firstOrNull { it.id == op.operationId }
        if (opFromList == null) {
            Assert.fail("Operation was not in the list")
            return
        }
        val opFuture = CompletableFuture<Any?>()
        ops.rejectOperation(opFromList, RejectionData("UNEXPECTED_OPERATION")) { result ->
            result.onSuccess { opFuture.complete(null) }
                .onFailure { opFuture.completeExceptionally(it) }
        }
        Assert.assertNull(opFuture.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testOperationPolling() {
        Assert.assertFalse(ops.isPollingOperations())
        var loadingCount = 0
        val future = CompletableFuture<Any?>()
        ops.listener = object : IOperationsServiceListener {
            override fun operationsLoading(loading: Boolean) {
                if (loading) {
                    loadingCount += 1
                    if (loadingCount == 3) {
                        ops.stopPollingOperations()
                        future.complete(null)
                    }
                }
            }
            override fun operationsChanged(
                operations: List<UserOperation>,
                removed: List<UserOperation>,
                added: List<UserOperation>
            ) {
            }
            override fun operationsFailed(error: ApiError) {
            }
        }
        ops.startPollingOperations()
        Assert.assertNull(future.get(30, TimeUnit.SECONDS))
        ops.stopPollingOperations()
        Assert.assertFalse(ops.isPollingOperations())
    }

    @Test
    fun testOperationHistory() {
        // lets create 1 operation and leave it in the state of "pending"
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val future = CompletableFuture<List<UserOperation>?>()
        ops.getHistory(auth) { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }

        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertNotNull("Operations not retrieved", operations)
        if (operations == null) {
            return
        }

        val opRecord = operations.firstOrNull { it.id == op.operationId }
        Assert.assertNotNull(opRecord)
        Assert.assertTrue(opRecord?.status == UserOperationStatus.PENDING)
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

    @Test
    fun testDetail() {
        val op = IntegrationUtils.createNonPersonalizedPACOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<UserOperation>()

        ops.getDetail(op.operationId) { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }

        val operation = future.get(20, TimeUnit.SECONDS)
        Assert.assertTrue("Failed to create & get the operation", operation != null)
        Assert.assertEquals("Operations ids are not equal", op.operationId, operation.id)
    }

    @Test
    fun testClaim() {
        val op = IntegrationUtils.createNonPersonalizedPACOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<UserOperation>()

        ops.claim(op.operationId) { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }

        val operation = future.get(20, TimeUnit.SECONDS)

        Assert.assertEquals("Incorrect type of preapproval screen", operation.ui?.preApprovalScreen?.type, PreApprovalScreen.Type.QR_SCAN)

        val totp = IntegrationUtils.getOperation(op).proximityOtp
        Assert.assertNotNull("Even with proximityCheckEnabled: true, in proximityOtp nil", totp)

        operation.proximityCheck = ProximityCheck(totp!!, ProximityCheckType.QR_CODE)

        val authorizedFuture = CompletableFuture<UserOperation?>()
        var auth = PowerAuthAuthentication.possessionWithPassword("xxxx") // wrong password on purpose

        ops.authorizeOperation(operation, auth) { result ->
            result.onSuccess { authorizedFuture.completeExceptionally(Exception("Operation should not be authorized")) }
                .onFailure { authorizedFuture.complete(null) }
        }
        Assert.assertNull(authorizedFuture.get(20, TimeUnit.SECONDS))

        auth = PowerAuthAuthentication.possessionWithPassword(pin)
        val authorizedFuture2 = CompletableFuture<Any?>()
        ops.authorizeOperation(operation, auth) { result ->
            result.onSuccess { authorizedFuture2.complete(null) }
                .onFailure { authorizedFuture2.completeExceptionally(it) }
        }
        Assert.assertNull(authorizedFuture2.get(20, TimeUnit.SECONDS))
    }

    @Test
    fun testOperationCanceledWithReason() {
        // create regular operation
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val cancelReason = "PREARRANGED_REASON"
        // cancel the operation
        IntegrationUtils.cancelOperation(op.operationId, cancelReason)

        val future = CompletableFuture<List<UserOperation>?>()
        val auth = PowerAuthAuthentication.possessionWithPassword(pin)
        ops.getHistory(auth) { result ->
            result.onSuccess { future.complete(it) }
                .onFailure { future.completeExceptionally(it) }
        }

        val operations = future.get(20, TimeUnit.SECONDS)
        Assert.assertNotNull("Operations not retrieved", operations)
        if (operations == null) {
            return
        }

        val opRecord = operations.firstOrNull { it.id == op.operationId }
        Assert.assertNotNull(opRecord)
        Assert.assertTrue("${opRecord?.statusReason} should be PREARRANGED_REASON", opRecord?.statusReason == "PREARRANGED_REASON")
    }
}
