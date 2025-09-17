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

import com.wultra.android.mtokensdk.api.operation.model.ProximityCheck
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheckType
import com.wultra.android.mtokensdk.api.operation.model.QROperationParser
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.api.operation.model.UserOperationStatus
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import com.wultra.android.mtokensdk.operation.*
import com.wultra.android.mtokensdk.operation.RejectionData
import com.wultra.android.mtokensdk.push.PushData
import com.wultra.android.mtokensdk.push.PushService
import com.wultra.android.powerauth.networking.error.ApiError
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.String

/**
 * Integration tests are calling a real backend server (based on configuration inside the "${ROOT_FOLDER}/configs/integration-tests.properties" file).
 */
class IntegrationTests {

    private lateinit var push: PushService
    private lateinit var ops: OperationsService
    private lateinit var pa: PowerAuthSDK
    private val pin = "1234"

    @Before
    fun setup() {
        try {
            val result = IntegrationUtils.prepareActivation(pin)
            pa = result.first
            ops = result.second.operations
            push = result.second.push
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
    fun testMobileTokenData() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)
        val future = CompletableFuture<Any?>()
        ops.getDetail(op.operationId) { result ->

            result.onFailure { future.completeExceptionally(it) }
                .onSuccess { detail ->
                    detail.mobileTokenData = mapOf(
                        "test1" to 1,
                        "test2" to 2.3,
                        "test3" to "string",
                        "test4" to mapOf(
                            "nested" to true
                        )
                    )

                    ops.authorizeOperation(detail, PowerAuthAuthentication.possessionWithPassword(pin)) { authResult ->
                        authResult.onFailure { future.completeExceptionally(it) }
                            .onSuccess {
                                val finalOp = IntegrationUtils.getOperation(op.operationId)
                                val serverMtd = finalOp.additionalData?.get("mobileTokenData") as? Map<String, Any> ?: throw Exception("mobileTokenData not found in additionalData")
                                val test1 = serverMtd["test1"]
                                val test2 = serverMtd["test2"]
                                val test3 = serverMtd["test3"]
                                val test4 = (serverMtd["test4"] as? Map<String, Any>)?.get("nested")

                                Assert.assertEquals(1.0, test1) // server returns as Double 🤷‍♂️
                                Assert.assertEquals(2.3, test2)
                                Assert.assertEquals("string", test3)
                                Assert.assertEquals(true, test4)
                                future.complete(null)
                            }
                    }
                }
        }
        Assert.assertNull(future.get(20, TimeUnit.SECONDS))
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
    fun testRejectPaymentWithAdditionalData() {
        val op = IntegrationUtils.createOperation(IntegrationUtils.Companion.Factors.F_2FA)

        // Fetch the operation from the list (same as your other test)
        val listFuture = CompletableFuture<List<UserOperation>>()
        ops.getOperations { result ->
            result.onSuccess { listFuture.complete(it) }
                .onFailure { listFuture.completeExceptionally(it) }
        }
        val operations = listFuture.get(20, TimeUnit.SECONDS)
        val opFromList = operations.firstOrNull { it.id == op.operationId }
            ?: run { Assert.fail("Operation was not in the list"); return }

        // Prepare rejection with additional mobileTokenData
        opFromList.mobileTokenData = mapOf(
            "test1" to 1,
            "test2" to 2.3,
            "test3" to "string",
            "test4" to mapOf("nested" to true)
        )

        val opFuture = CompletableFuture<Any?>()
        ops.rejectOperation(opFromList, RejectionData("POSSIBLE_FRAUD")) { result ->
            result.onFailure { opFuture.completeExceptionally(it) }
                .onSuccess {
                    //TODO: uncoment when BE ready
//                    val finalOp = IntegrationUtils.getOperation(op.operationId)
//                    val serverMtd = finalOp.additionalData?.get("mobileTokenData") as? Map<String, Any> ?: throw Exception("mobileTokenData not found in additionalData")
//                    val test1 = serverMtd["test1"]
//                    val test2 = serverMtd["test2"]
//                    val test3 = serverMtd["test3"]
//                    val test4 = (serverMtd["test4"] as? Map<String, Any>)?.get("nested")
//
//                    Assert.assertEquals(1.0, test1) // server returns as Double 🤷‍♂️
//                    Assert.assertEquals(2.3, test2)
//                    Assert.assertEquals("string", test3)
//                    Assert.assertEquals(true, test4)
                    opFuture.complete(null)
                }

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

        Assert.assertEquals("Incorrect type of preapproval screen", operation.ui?.preApprovalScreens?.get(0)?.type, PreApprovalScreen.Type.QR_SCAN)

        val totp = IntegrationUtils.getOperation(op.operationId).proximityOtp
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

    @Test
    fun testRegisterPushLegacyAndroid() {
        val future = CompletableFuture<Any?>()
        push.register("testToken") { result ->
            result.onSuccess { future.complete(null) }
                .onFailure { future.completeExceptionally(it) }
        }
        Assert.assertNull(future.get(10, TimeUnit.SECONDS))
    }

    @Test
    fun testRegisterPushLegacyHuawei() {
        val future = CompletableFuture<Any?>()
        push.registerHuawei("testToken") { result ->
            result.onSuccess { future.complete(null) }
                .onFailure { future.completeExceptionally(it) }
        }
        Assert.assertNull(future.get(10, TimeUnit.SECONDS))
    }

    @Test
    fun testRegisterPushFCM() {
        val future = CompletableFuture<Any?>()
        push.register(PushData.fcm("testToken")) { result ->
            result.onSuccess { future.complete(null) }
                .onFailure { future.completeExceptionally(it) }
        }
        Assert.assertNull(future.get(10, TimeUnit.SECONDS))
    }

    @Test
    fun testRegisterPushHMS() {
        val future = CompletableFuture<Any?>()
        push.register(PushData.hms("testToken")) { result ->
            result.onSuccess { future.complete(null) }
                .onFailure { future.completeExceptionally(it) }
        }
        Assert.assertNull(future.get(10, TimeUnit.SECONDS))
    }
}
