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
    fun `test authorize request without mobile token data`() {
        val operation = TestOperation("test-id", "test-data")
        val request = AuthorizeRequestObject(operation)

        val json = gson.toJson(request)

        Assert.assertTrue("JSON should contain id", json.contains("\"id\":\"test-id\""))
        Assert.assertTrue("JSON should contain data", json.contains("\"data\":\"test-data\""))
        Assert.assertFalse("JSON should not contain mobileTokenData when null", json.contains("mobileTokenData"))
    }

    @Test
    fun `test authorize request with mobile token data`() {
        val mobileTokenData = mapOf(
            "deviceFingerprint" to "abc123def456",
            "riskScore" to 0.8
        )
        val operation = TestOperation("test-id", "test-data", mobileTokenData = mobileTokenData)
        val request = AuthorizeRequestObject(operation)

        val json = gson.toJson(request)

        Assert.assertTrue("JSON should contain id", json.contains("\"id\":\"test-id\""))
        Assert.assertTrue("JSON should contain data", json.contains("\"data\":\"test-data\""))
        Assert.assertTrue("JSON should contain mobileTokenData", json.contains("\"mobileTokenData\""))
        Assert.assertTrue("JSON should contain deviceFingerprint", json.contains("\"deviceFingerprint\":\"abc123def456\""))
        Assert.assertTrue("JSON should contain riskScore", json.contains("\"riskScore\":0.8"))
    }

    @Test
    fun `test authorize request primary constructor with mobile token data`() {
        val mobileTokenData = mapOf("testKey" to "testValue")
        val request = AuthorizeRequestObject("op-id", "op-data", null, mobileTokenData)

        val json = gson.toJson(request)

        Assert.assertTrue("JSON should contain mobileTokenData", json.contains("\"mobileTokenData\""))
        Assert.assertTrue("JSON should contain testKey", json.contains("\"testKey\":\"testValue\""))
    }

    @Test
    fun `test authorize request with proximity check data`() {
        val timestamp = java.time.ZonedDateTime.now()
        val proximityCheckData = ProximityCheckData(
            otp = "123456",
            type = ProximityCheckType.QR_CODE,
            timestampReceived = timestamp,
            timestampSent = timestamp
        )
        val operation = TestOperation("test-id", "test-data")
        val request = AuthorizeRequestObject(operation, proximityCheckData)

        val json = gson.toJson(request)

        Assert.assertTrue("JSON should contain otp", json.contains("\"otp\":\"123456\""))
        Assert.assertTrue("JSON should contain type", json.contains("\"type\":\"QR_CODE\""))
        Assert.assertTrue("JSON should contain timestampReceived", json.contains("\"timestampReceived\""))
        Assert.assertTrue("JSON should contain timestampSent", json.contains("\"timestampSent\""))
    }

    @Test
    fun `test authorize request without proximity check data`() {
        val operation = TestOperation("test-id", "test-data")
        val request = AuthorizeRequestObject(operation, null)

        val json = gson.toJson(request)

        Assert.assertFalse("JSON should not contain proximityCheck when null", json.contains("proximityCheck"))
    }
}
