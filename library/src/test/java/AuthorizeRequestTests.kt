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

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.Gson
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.operation.OperationsUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthorizeRequestTests {

    private lateinit var gson: Gson

    @Before
    fun prepareGson() {
        gson = OperationsUtils.defaultGsonBuilder().create()
    }

    private class TestOperation(
        override val id: String,
        override val data: String,
        override var proximityCheck: ProximityCheck? = null,
        override var mobileTokenData: Map<String, Any>? = null
    ) : IOperation

    @Test
    fun `test authorize request without optional data omits them`() {
        val request = AuthorizeRequestObject(TestOperation("test-id", "test-data"))

        val json = gson.toJson(request)

        Assert.assertTrue(json, json.contains("\"id\":\"test-id\""))
        Assert.assertTrue(json, json.contains("\"data\":\"test-data\""))
        Assert.assertFalse(json, json.contains("mobileTokenData"))
        Assert.assertFalse(json, json.contains("proximityCheck"))
    }

    @Test
    fun `test authorize request takes mobile token data from operation`() {
        val mobileTokenData = mapOf(
            "deviceFingerprint" to "abc123def456",
            "riskScore" to 0.8
        )
        val request = AuthorizeRequestObject(TestOperation("test-id", "test-data", mobileTokenData = mobileTokenData))

        val json = gson.toJson(request)

        Assert.assertTrue(json, json.contains("\"mobileTokenData\":{\"deviceFingerprint\":\"abc123def456\",\"riskScore\":0.8}"))
    }

    @Test
    fun `test proximity check serializes with UTC instant timestamps`() {
        val timestamp = java.time.ZonedDateTime.of(2026, 10, 21, 13, 35, 56, 898_765_000, java.time.ZoneId.of("Europe/Prague"))
        val proximityCheckData = ProximityCheckData(
            otp = "123456",
            type = ProximityCheckType.QR_CODE,
            timestampReceived = timestamp,
            timestampSent = timestamp
        )
        val request = AuthorizeRequestObject(TestOperation("test-id", "test-data"), proximityCheckData)

        val json = gson.toJson(request)

        Assert.assertTrue(json, json.contains("\"otp\":\"123456\""))
        Assert.assertTrue(json, json.contains("\"type\":\"QR_CODE\""))
        Assert.assertTrue(json, json.contains("\"timestampReceived\":\"2026-10-21T11:35:56.898Z\""))
        Assert.assertTrue(json, json.contains("\"timestampSent\":\"2026-10-21T11:35:56.898Z\""))
    }

    @Test
    fun `test customer ZonedDateTime in mobile token data keeps legacy format`() {
        val timestamp = java.time.ZonedDateTime.of(2026, 10, 21, 13, 35, 56, 898_765_000, java.time.ZoneId.of("Europe/Prague"))
        val request = AuthorizeRequestObject("op-id", "op-data", null, mapOf("customTime" to timestamp))

        val json = gson.toJson(request)

        Assert.assertTrue(json, json.contains("\"customTime\":\"2026-10-21T13:35:56.898Z\""))
    }
}
