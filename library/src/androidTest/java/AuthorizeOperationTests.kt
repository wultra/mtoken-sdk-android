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
import androidx.test.core.app.ApplicationProvider
import com.wultra.android.mtokensdk.api.operation.*
import com.wultra.android.mtokensdk.api.operation.model.IOperation
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheck
import com.wultra.android.mtokensdk.api.operation.model.ProximityCheckType
import com.wultra.android.mtokensdk.operation.OperationsService
import com.wultra.android.powerauth.networking.IApiCallResponseListener
import com.wultra.android.powerauth.networking.OkHttpBuilderInterceptor
import com.wultra.android.powerauth.networking.data.StatusResponse
import com.wultra.android.powerauth.networking.data.StatusResponse.Status
import io.getlime.security.powerauth.networking.interfaces.ICancelable
import io.getlime.security.powerauth.networking.response.ITimeSynchronizationListener
import io.getlime.security.powerauth.sdk.IPowerAuthTimeSynchronizationService
import io.getlime.security.powerauth.sdk.PowerAuthAuthentication
import io.getlime.security.powerauth.sdk.PowerAuthClientConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthConfiguration
import io.getlime.security.powerauth.sdk.PowerAuthSDK
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
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
 * Also tests timestamp adjustment logic (adjustProximityCheckData) indirectly.
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

        override fun isTimeSynchronized(): Boolean = synchronized
        override fun getCurrentTime(): Long = System.currentTimeMillis() + offsetMs
        override fun getLocalTimeAdjustment(): Long = offsetMs
        override fun getLocalTimeAdjustmentPrecision(): Long = 50

        override fun synchronizeTime(listener: ITimeSynchronizationListener): ICancelable? {
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
        var authorizeSuccess: Boolean = true

        override fun authorize(authorizeRequest: AuthorizeRequest, authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<StatusResponse>) {
            lastAuthorizeRequest = authorizeRequest
            if (authorizeSuccess) {
                listener.onSuccess(StatusResponse(Status.OK))
            } else {
                listener.onFailure(com.wultra.android.powerauth.networking.error.ApiError(RuntimeException("authorize failed")))
            }
        }

        override fun list(listener: IApiCallResponseListener<OperationListResponse>) {}
        override fun history(authentication: PowerAuthAuthentication, listener: IApiCallResponseListener<OperationHistoryResponse>) {}
        override fun reject(rejectRequest: RejectRequest, listener: IApiCallResponseListener<StatusResponse>) {}
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
        // Use reflection to set timestampReceived since it has internal set
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
}
