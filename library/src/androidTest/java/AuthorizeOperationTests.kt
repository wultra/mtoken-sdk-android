/*
 * Copyright 2025 Wultra s.r.o.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wultra.android.mtokensdk.test

import android.content.Context
import com.wultra.android.mtokensdk.api.operation.*
import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheck
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheckType
import com.wultra.android.mtokensdk.api.operation.model.mobiletokendata.MobileTokenData
import com.wultra.android.mtokensdk.api.operation.model.mobiletokendata.PreApprovalScreensRecorder
import com.wultra.android.mtokensdk.api.operation.model.mobiletokendata.ServerTimeAdjustable
import com.wultra.android.mtokensdk.operation.OperationsService
import com.wultra.android.mtokensdk.operation.RejectionData
import com.wultra.android.mtokensdk.operation.RejectionReason
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.data.StatusResponse.Status
import com.wultra.android.powerauth.networking.error.ApiErrorException
import io.getlime.security.powerauth.networking.interfaces.ICancelable
import io.getlime.security.powerauth.networking.response.ITimeSynchronizationListener
import io.getlime.security.powerauth.sdk.IPowerAuthTimeSynchronizationService
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Tests for [OperationsService.authorizeOperation] covering all branches:
 * - No proximity check → authorize directly (no timestamp adjustment)
 * - Proximity check + time already synchronized → adjust timestamps, then authorize
 * - Proximity check + time NOT synchronized → synchronize first, adjust, then authorize
 * - Proximity check + time synchronization FAILS → callback with failure
 *
 * Also tests timestamp adjustment logic (adjustProximityCheckData) indirectly, and alignment
 * of [PreApprovalScreensRecorder] timestamps during [OperationsService.authorizeOperation]
 * and [OperationsService.rejectOperation].
 */
class AuthorizeOperationTests {

    private lateinit var context: Context
    private lateinit var pa: PowerAuthSDK

    @Before
    fun setup() {
        val result = IntegrationUtils.prepareForAuthorize()
        pa = result.first
        context = result.second
    }

    // --- Mock: IOperation ---

    private class TestOperation(
        override val id: String = "test-op-1",
        override val data: String = "A1*A100CZK*ICZ2730300000001165254011*D20250101",
        override var proximityCheck: ProximityCheck? = null,
        override var mobileTokenData: Map<String, Any>? = null
    ) : IOperation

    // --- Mock: IPowerAuthTimeSynchronizationService ---

    /**
     * Mock time service that reports time as synchronized with configurable offset.
     * [offsetMs]: positive = server ahead of device, negative = server behind.
     */
    private class SynchronizedTimeService(
        private val offsetMs: Long = 0
    ) : IPowerAuthTimeSynchronizationService {

        override fun isTimeSynchronized(): Boolean = true
        override fun getCurrentTime(): Long = System.currentTimeMillis() + offsetMs
        override fun getLocalTimeAdjustment(): Long = offsetMs
        override fun getLocalTimeAdjustmentPrecision(): Long = 50

        override fun synchronizeTime(listener: ITimeSynchronizationListener): ICancelable? {
            listener.onTimeSynchronizationSucceeded()
            return null
        }

        override fun resetTimeSynchronization() {}
    }

    /**
     * Mock time service that reports NOT synchronized initially.
     * Calling [synchronizeTime] will succeed and flip state to synchronized.
     */
    private class DelayedSyncTimeService(
        private val offsetMs: Long = 0
    ) : IPowerAuthTimeSynchronizationService {

        private var synchronized = false
        var syncCount = 0
            private set

        override fun isTimeSynchronized(): Boolean = synchronized
        override fun getCurrentTime(): Long = System.currentTimeMillis() + offsetMs
        override fun getLocalTimeAdjustment(): Long = offsetMs
        override fun getLocalTimeAdjustmentPrecision(): Long = 50

        override fun synchronizeTime(listener: ITimeSynchronizationListener): ICancelable? {
            syncCount++
            synchronized = true
            listener.onTimeSynchronizationSucceeded()
            return null
        }

        override fun resetTimeSynchronization() {
            synchronized = false
        }
    }

    /**
     * Mock time service where synchronization always fails.
     */
    private class FailingSyncTimeService : IPowerAuthTimeSynchronizationService {

        override fun isTimeSynchronized(): Boolean = false
        override fun getCurrentTime(): Long = System.currentTimeMillis()
        override fun getLocalTimeAdjustment(): Long = 0
        override fun getLocalTimeAdjustmentPrecision(): Long = 0

        override fun synchronizeTime(listener: ITimeSynchronizationListener): ICancelable? {
            listener.onTimeSynchronizationFailed(RuntimeException("Time sync unavailable"))
            return null
        }

        override fun resetTimeSynchronization() {}
    }

    // --- Mock: IOperationApi ---

    /**
     * Mock API that captures authorize requests and immediately reports success.
     */
    private class MockOperationApi : IOperationApi {
        override var acceptLanguage: String = "en"
        override var okHttpInterceptor: OkHttpBuilderInterceptor? = null

        var lastAuthorizeRequest: AuthorizeRequest? = null
        val authorizeRequests = mutableListOf<AuthorizeRequest>()
        var authorizeSuccess: Boolean = true

        override fun authorize(authorizeRequest: AuthorizeRequest, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<StatusResponse>) {
            lastAuthorizeRequest = authorizeRequest
            authorizeRequests += authorizeRequest
            if (authorizeSuccess) {
                listener.onSuccess(StatusResponse(Status.OK))
            } else {
                listener.onFailure(com.wultra.android.powerauth.networking.error.ApiError(RuntimeException("authorize failed")))
            }
        }

        var lastRejectRequest: RejectRequest? = null

        override fun list(listener: IApiCallResponseListener<OperationListResponse>) {}
        override fun history(authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<OperationHistoryResponse>) {}
        override fun reject(rejectRequest: RejectRequest, listener: IApiCallResponseListener<StatusResponse>) {
            lastRejectRequest = rejectRequest
            listener.onSuccess(StatusResponse(Status.OK))
        }
        override fun getDetail(claimRequest: OperationClaimDetailRequest, listener: IApiCallResponseListener<OperationClaimDetailResponse>) {}
        override fun claim(claimRequest: OperationClaimDetailRequest, listener: IApiCallResponseListener<OperationClaimDetailResponse>) {}
    }

    // --- Helper ---

    private fun createService(mockApi: MockOperationApi, timeService: IPowerAuthTimeSynchronizationService): OperationsService {
        return OperationsService(pa, context, mockApi, timeService)
    }

    // --- Tests ---

    @Test
    fun testAuthorizeWithoutProximityCheck() {
        val mockApi = MockOperationApi()
        val timeService = SynchronizedTimeService()
        val service = createService(mockApi, timeService)

        val operation = TestOperation()
        val future = CompletableFuture<Result<Unit>>()

        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue("Should succeed", result.isSuccess)

        val request = mockApi.lastAuthorizeRequest
        Assert.assertNotNull("Request should be captured", request)
        Assert.assertNull("proximityCheck should be null in request", request!!.requestObject.proximityCheck)
    }

    @Test
    fun testAuthorizeWithProximityCheckTimeSynchronized() {
        val offsetMs = 5 * 60 * 1000L // 5 minutes ahead
        val mockApi = MockOperationApi()
        val timeService = SynchronizedTimeService(offsetMs)
        val service = createService(mockApi, timeService)

        val operation = TestOperation()
        operation.proximityCheck = ProximityCheck("123456", ProximityCheckType.QR_CODE)

        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue("Should succeed", result.isSuccess)

        val request = mockApi.lastAuthorizeRequest
        Assert.assertNotNull("Request should be captured", request)
        val pcData = request!!.requestObject.proximityCheck
        Assert.assertNotNull("proximityCheck data should be present", pcData)
        Assert.assertEquals("OTP should match", "123456", pcData!!.otp)
        Assert.assertEquals("Type should match", ProximityCheckType.QR_CODE, pcData.type)

        // timestampReceived should be adjusted by offsetMs (shifted forward)
        val now = System.currentTimeMillis()
        val receivedMs = pcData.timestampReceived.toInstant().toEpochMilli()
        Assert.assertTrue(
            "timestampReceived should be ~5min ahead of device time, got delta=${receivedMs - now}ms",
            receivedMs > now + offsetMs - 2000 && receivedMs < now + offsetMs + 2000
        )

        // timestampSent should be server's current time (~now + offsetMs)
        val sentMs = pcData.timestampSent.toInstant().toEpochMilli()
        Assert.assertTrue(
            "timestampSent should be ~5min ahead of device time, got delta=${sentMs - now}ms",
            sentMs > now + offsetMs - 2000 && sentMs < now + offsetMs + 2000
        )
    }

    @Test
    fun testAuthorizeWithProximityCheckTimeNotSynchronizedThenSyncs() {
        val offsetMs = -3 * 60 * 1000L // device 3 minutes ahead
        val mockApi = MockOperationApi()
        val timeService = DelayedSyncTimeService(offsetMs)
        val service = createService(mockApi, timeService)

        val operation = TestOperation()
        operation.proximityCheck = ProximityCheck("654321", ProximityCheckType.DEEPLINK)

        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue("Should succeed after sync", result.isSuccess)

        val pcData = mockApi.lastAuthorizeRequest!!.requestObject.proximityCheck
        Assert.assertNotNull("proximityCheck data should be present", pcData)
        Assert.assertEquals("654321", pcData!!.otp)
        Assert.assertEquals(ProximityCheckType.DEEPLINK, pcData.type)

        // Verify timestamps adjusted with negative offset (shifted backward)
        val now = System.currentTimeMillis()
        val receivedMs = pcData.timestampReceived.toInstant().toEpochMilli()
        Assert.assertTrue(
            "timestampReceived should be ~3min behind device time, got delta=${receivedMs - now}ms",
            receivedMs > now + offsetMs - 2000 && receivedMs < now + offsetMs + 2000
        )
    }

    @Test
    fun testAuthorizeWithProximityCheckTimeSyncFails() {
        val mockApi = MockOperationApi()
        val timeService = FailingSyncTimeService()
        val service = createService(mockApi, timeService)

        val operation = TestOperation()
        operation.proximityCheck = ProximityCheck("111111", ProximityCheckType.QR_CODE)

        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue("Should fail when time sync fails", result.isFailure)
        Assert.assertTrue(
            "Error should indicate sync failure",
            result.exceptionOrNull()!!.message!!.contains("Time sync unavailable")
        )
        assertTimeSyncFailure(result)

        // API should NOT have been called
        Assert.assertNull("API should not be called when sync fails", mockApi.lastAuthorizeRequest)
    }

    @Test
    fun testTimestampAdjustmentWithZeroOffset() {
        val mockApi = MockOperationApi()
        val timeService = SynchronizedTimeService(offsetMs = 0)
        val service = createService(mockApi, timeService)

        val operation = TestOperation()
        operation.proximityCheck = ProximityCheck("000000", ProximityCheckType.QR_CODE)

        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue(result.isSuccess)

        val pcData = mockApi.lastAuthorizeRequest!!.requestObject.proximityCheck!!
        val now = System.currentTimeMillis()
        val receivedMs = pcData.timestampReceived.toInstant().toEpochMilli()
        val sentMs = pcData.timestampSent.toInstant().toEpochMilli()

        // With zero offset, timestamps should be close to system time
        Assert.assertTrue(
            "timestampReceived should be close to device time with zero offset",
            Math.abs(receivedMs - now) < 2000
        )
        Assert.assertTrue(
            "timestampSent should be close to device time with zero offset",
            Math.abs(sentMs - now) < 2000
        )
    }

    @Test
    fun testTimestampAdjustmentWithLargePositiveOffset() {
        val oneHourMs = 60 * 60 * 1000L
        val mockApi = MockOperationApi()
        val timeService = SynchronizedTimeService(offsetMs = oneHourMs)
        val service = createService(mockApi, timeService)

        val operation = TestOperation()
        operation.proximityCheck = ProximityCheck("999999", ProximityCheckType.DEEPLINK)

        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue(result.isSuccess)

        val pcData = mockApi.lastAuthorizeRequest!!.requestObject.proximityCheck!!
        val now = System.currentTimeMillis()
        val receivedMs = pcData.timestampReceived.toInstant().toEpochMilli()
        val sentMs = pcData.timestampSent.toInstant().toEpochMilli()

        // With 1 hour offset, both timestamps should be ~1h ahead
        Assert.assertTrue(
            "timestampReceived should be ~1h ahead, got delta=${receivedMs - now}ms",
            receivedMs > now + oneHourMs - 2000 && receivedMs < now + oneHourMs + 2000
        )
        Assert.assertTrue(
            "timestampSent should be ~1h ahead, got delta=${sentMs - now}ms",
            sentMs > now + oneHourMs - 2000 && sentMs < now + oneHourMs + 2000
        )
    }

    @Test
    fun testTimestampReceivedPreservesOriginalTimeDelta() {
        val offsetMs = 10_000L // 10 seconds
        val mockApi = MockOperationApi()
        val timeService = SynchronizedTimeService(offsetMs)
        val service = createService(mockApi, timeService)

        // Create proximity check with a specific "received" time (e.g., 30 seconds ago)
        val operation = TestOperation()
        val pc = ProximityCheck("abc123", ProximityCheckType.QR_CODE)
        // Simulate that the proximity check was received 30 seconds ago in device time
        val thirtySecondsAgo = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(System.currentTimeMillis() - 30_000),
            ZoneId.systemDefault()
        )
        // Use reflection to set timestampReceived to simulate a QR scan 30 seconds ago
        val field = ProximityCheck::class.java.getDeclaredField("timestampReceived")
        field.isAccessible = true
        field.set(pc, thirtySecondsAgo)
        operation.proximityCheck = pc

        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { result ->
            future.complete(result)
        }

        val result = future.get(5, TimeUnit.SECONDS)
        Assert.assertTrue(result.isSuccess)

        val pcData = mockApi.lastAuthorizeRequest!!.requestObject.proximityCheck!!
        val now = System.currentTimeMillis()

        // timestampReceived was 30s ago in device time, now adjusted by +10s offset
        // So it should be ~20 seconds behind the current server time
        val receivedMs = pcData.timestampReceived.toInstant().toEpochMilli()
        val expectedReceived = now - 30_000 + offsetMs // original - 30s + adjustment 10s = -20s from now
        Assert.assertTrue(
            "timestampReceived should be original time adjusted by offset. Expected ~$expectedReceived, got $receivedMs",
            Math.abs(receivedMs - expectedReceived) < 2000
        )

        // timestampSent should be current server time (~now + offsetMs)
        val sentMs = pcData.timestampSent.toInstant().toEpochMilli()
        Assert.assertTrue(
            "timestampSent should be current server time",
            Math.abs(sentMs - (now + offsetMs)) < 2000
        )

        // The difference between sent and received should be ~30 seconds
        // (the time between when the QR was scanned and when authorize was called)
        val delta = sentMs - receivedMs
        Assert.assertTrue(
            "Delta between sent and received should be ~30s, got ${delta}ms",
            delta in 28_000..32_000
        )
    }

    @Test
    fun testAuthorizeRetryDoesNotDoubleAdjustProximityCheck() {
        val offsetMs = 10_000L
        val mockApi = MockOperationApi()
        val service = createService(mockApi, SynchronizedTimeService(offsetMs))

        val originalReceived = ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() - 30_000), ZoneId.systemDefault())
        val pc = ProximityCheck("123456", ProximityCheckType.QR_CODE)
        pc.timestampReceived = originalReceived
        val operation = TestOperation(proximityCheck = pc)

        // First attempt fails at the API (e.g. wrong password), the user retries
        mockApi.authorizeSuccess = false
        Assert.assertTrue("First attempt should fail", authorize(service, operation).isFailure)
        mockApi.authorizeSuccess = true
        Assert.assertTrue("Retry should succeed", authorize(service, operation).isSuccess)

        Assert.assertEquals(2, mockApi.authorizeRequests.size)
        val expectedReceived = originalReceived.toInstant().plusMillis(offsetMs)
        mockApi.authorizeRequests.forEachIndexed { index, request ->
            Assert.assertEquals(
                "Request #$index timestampReceived should be adjusted exactly once",
                expectedReceived,
                request.requestObject.proximityCheck!!.timestampReceived.toInstant()
            )
        }
        Assert.assertEquals("Operation ProximityCheck should not be modified", originalReceived, operation.proximityCheck!!.timestampReceived)
    }

    // --- Mobile token data with PreApprovalScreensRecorder ---

    private fun Any.field(name: String): Any? = javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)

    private fun mobileTokenDataWithRecorder(): Map<String, Any> {
        val recorder = PreApprovalScreensRecorder()
            .begin("intro")
            .end("intro", PreApprovalScreensRecorder.Action.CONTINUE)
            .begin("qr")
            .end("qr", PreApprovalScreensRecorder.Action.SCAN)
        val customerData = mapOf<String, Any>(
            "baseK" to "baseV",
            "customTime" to ZonedDateTime.of(2026, 10, 21, 13, 35, 56, 0, ZoneId.of("Europe/Prague")),
            "customNested" to mapOf("time" to ZonedDateTime.now(), "list" to listOf(1, 2))
        )
        return MobileTokenData.Builder(customerData)
            .put(recorder)
            .build()
    }

    /** Asserts that recorder timestamps in [sent] are shifted by [offsetMs] compared to [original]. */
    private fun assertRecorderAligned(original: Map<String, Any>, sent: Map<String, Any>?, offsetMs: Long) {
        Assert.assertNotNull("mobileTokenData should be sent", sent)
        Assert.assertEquals("Keys should be preserved", original.keys, sent!!.keys)
        original.filterKeys { it != "preApprovalScreens" }.forEach { (key, value) ->
            Assert.assertSame("Entry '$key' should be untouched", value, sent[key])
        }

        val rawVisits = original["preApprovalScreens"]
        val sentVisits = sent["preApprovalScreens"]
        Assert.assertTrue("Original mobileTokenData should not be modified", rawVisits is ServerTimeAdjustable)
        Assert.assertFalse("Sent visits should be aligned", sentVisits is ServerTimeAdjustable)

        rawVisits as List<*>
        sentVisits as List<*>
        Assert.assertEquals(rawVisits.size, sentVisits.size)
        rawVisits.zip(sentVisits).forEach { (raw, aligned) ->
            Assert.assertEquals(raw!!.field("screen"), aligned!!.field("screen"))
            Assert.assertEquals(raw.field("action"), aligned.field("action"))
            listOf("timestampOpened", "timestampClosed").forEach { name ->
                val rawTimestamp = raw.field(name) as ZonedDateTime
                val alignedTimestamp = aligned.field(name) as ZonedDateTime
                Assert.assertEquals("$name should be shifted by offset", offsetMs, Duration.between(rawTimestamp, alignedTimestamp).toMillis())
            }
        }
    }

    private fun authorize(service: OperationsService, operation: IOperation): Result<Unit> {
        val future = CompletableFuture<Result<Unit>>()
        service.authorizeOperation(operation, PowerAuthAuthentication.possession()) { future.complete(it) }
        return future.get(5, TimeUnit.SECONDS)
    }

    private fun reject(service: OperationsService, operation: IOperation): Result<Unit> {
        val future = CompletableFuture<Result<Unit>>()
        service.rejectOperation(operation, RejectionData(RejectionReason.UNEXPECTED_OPERATION)) { future.complete(it) }
        return future.get(5, TimeUnit.SECONDS)
    }

    @Test
    fun testAuthorizeAlignsRecorderTimestampsWhenTimeSynchronized() {
        val offsetMs = 5 * 60 * 1000L
        val mockApi = MockOperationApi()
        val service = createService(mockApi, SynchronizedTimeService(offsetMs))
        val mobileTokenData = mobileTokenDataWithRecorder()
        val operation = TestOperation(mobileTokenData = mobileTokenData)

        Assert.assertTrue("Should succeed", authorize(service, operation).isSuccess)

        val requestObject = mockApi.lastAuthorizeRequest!!.requestObject
        Assert.assertNull("proximityCheck should be null in request", requestObject.proximityCheck)
        assertRecorderAligned(mobileTokenData, requestObject.mobileTokenData, offsetMs)
        Assert.assertSame("Operation mobileTokenData should not be replaced", mobileTokenData, operation.mobileTokenData)
    }

    @Test
    fun testAuthorizeSynchronizesTimeForRecorderData() {
        val offsetMs = -3 * 60 * 1000L
        val mockApi = MockOperationApi()
        val timeService = DelayedSyncTimeService(offsetMs)
        val service = createService(mockApi, timeService)
        val mobileTokenData = mobileTokenDataWithRecorder()

        Assert.assertTrue("Should succeed", authorize(service, TestOperation(mobileTokenData = mobileTokenData)).isSuccess)

        Assert.assertEquals("Time should be synchronized once", 1, timeService.syncCount)
        assertRecorderAligned(mobileTokenData, mockApi.lastAuthorizeRequest!!.requestObject.mobileTokenData, offsetMs)
    }

    @Test
    fun testAuthorizeWithRecorderDataTimeSyncFails() {
        val mockApi = MockOperationApi()
        val service = createService(mockApi, FailingSyncTimeService())

        val result = authorize(service, TestOperation(mobileTokenData = mobileTokenDataWithRecorder()))

        Assert.assertTrue("Should fail when time sync fails", result.isFailure)
        assertTimeSyncFailure(result)
        Assert.assertNull("API should not be called when sync fails", mockApi.lastAuthorizeRequest)
    }

    @Test
    fun testAuthorizeWithoutTimeSensitiveDataSkipsTimeSync() {
        val mockApi = MockOperationApi()
        val service = createService(mockApi, FailingSyncTimeService())
        val mobileTokenData = mapOf<String, Any>("k" to "v")

        Assert.assertTrue("Should succeed without time sync", authorize(service, TestOperation(mobileTokenData = mobileTokenData)).isSuccess)
        Assert.assertEquals(mobileTokenData, mockApi.lastAuthorizeRequest!!.requestObject.mobileTokenData)
    }

    @Test
    fun testAuthorizeWithProximityCheckAndRecorderDataSynchronizesOnce() {
        val offsetMs = 2 * 60 * 1000L
        val mockApi = MockOperationApi()
        val timeService = DelayedSyncTimeService(offsetMs)
        val service = createService(mockApi, timeService)
        val mobileTokenData = mobileTokenDataWithRecorder()
        val operation = TestOperation(
            proximityCheck = ProximityCheck("123456", ProximityCheckType.QR_CODE),
            mobileTokenData = mobileTokenData
        )

        Assert.assertTrue("Should succeed", authorize(service, operation).isSuccess)

        Assert.assertEquals("Time should be synchronized once", 1, timeService.syncCount)
        val requestObject = mockApi.lastAuthorizeRequest!!.requestObject
        Assert.assertNotNull("proximityCheck data should be present", requestObject.proximityCheck)
        assertRecorderAligned(mobileTokenData, requestObject.mobileTokenData, offsetMs)
    }

    @Test
    fun testRejectAlignsRecorderTimestampsWhenTimeSynchronized() {
        val offsetMs = 5 * 60 * 1000L
        val mockApi = MockOperationApi()
        val service = createService(mockApi, SynchronizedTimeService(offsetMs))
        val mobileTokenData = mobileTokenDataWithRecorder()
        val operation = TestOperation(mobileTokenData = mobileTokenData)

        Assert.assertTrue("Should succeed", reject(service, operation).isSuccess)

        val requestObject = mockApi.lastRejectRequest!!.requestObject
        Assert.assertEquals(RejectionReason.UNEXPECTED_OPERATION.reason, requestObject.reason)
        assertRecorderAligned(mobileTokenData, requestObject.mobileTokenData, offsetMs)
        Assert.assertSame("Operation mobileTokenData should not be replaced", mobileTokenData, operation.mobileTokenData)
    }

    @Test
    fun testRejectSynchronizesTimeForRecorderData() {
        val offsetMs = -3 * 60 * 1000L
        val mockApi = MockOperationApi()
        val timeService = DelayedSyncTimeService(offsetMs)
        val service = createService(mockApi, timeService)
        val mobileTokenData = mobileTokenDataWithRecorder()

        Assert.assertTrue("Should succeed", reject(service, TestOperation(mobileTokenData = mobileTokenData)).isSuccess)

        Assert.assertEquals("Time should be synchronized once", 1, timeService.syncCount)
        assertRecorderAligned(mobileTokenData, mockApi.lastRejectRequest!!.requestObject.mobileTokenData, offsetMs)
    }

    @Test
    fun testAuthorizeDoesNotAlignNestedRecorderData() {
        val mockApi = MockOperationApi()
        val timeService = DelayedSyncTimeService(60_000L)
        val service = createService(mockApi, timeService)
        val recorderData = PreApprovalScreensRecorder().begin("s1").end("s1", PreApprovalScreensRecorder.Action.CONTINUE).build()
        val nested = mapOf("fds" to mapOf("screens" to recorderData))
        val mobileTokenData = mapOf<String, Any>("custom" to nested)

        Assert.assertTrue("Should succeed", authorize(service, TestOperation(mobileTokenData = mobileTokenData)).isSuccess)

        Assert.assertEquals("Nested data should not trigger time sync", 0, timeService.syncCount)
        Assert.assertSame("Nested data should be sent as-is", mobileTokenData, mockApi.lastAuthorizeRequest!!.requestObject.mobileTokenData)
    }

    private fun assertTimeSyncFailure(result: Result<Unit>) {
        val error = result.exceptionOrNull()
        Assert.assertTrue("Error should be ApiErrorException, was $error", error is ApiErrorException)
        Assert.assertEquals("Time sync unavailable", error!!.cause?.message)
    }

    @Test
    fun testRejectWithRecorderDataTimeSyncFails() {
        val mockApi = MockOperationApi()
        val service = createService(mockApi, FailingSyncTimeService())

        val result = reject(service, TestOperation(mobileTokenData = mobileTokenDataWithRecorder()))

        Assert.assertTrue("Should fail when time sync fails", result.isFailure)
        assertTimeSyncFailure(result)
        Assert.assertNull("API should not be called when sync fails", mockApi.lastRejectRequest)
    }

    @Test
    fun testRejectWithoutTimeSensitiveDataSkipsTimeSync() {
        val mockApi = MockOperationApi()
        val service = createService(mockApi, FailingSyncTimeService())
        val mobileTokenData = mapOf<String, Any>("k" to "v")

        Assert.assertTrue("Should succeed without time sync", reject(service, TestOperation(mobileTokenData = mobileTokenData)).isSuccess)
        Assert.assertEquals(mobileTokenData, mockApi.lastRejectRequest!!.requestObject.mobileTokenData)
    }
}
