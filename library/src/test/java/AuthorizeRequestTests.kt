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
import com.google.gson.GsonBuilder
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.operation.OperationsUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class AuthorizeRequestTests {

    private lateinit var gson: Gson

    @Before
    fun prepareGson() {
        gson = OperationsUtils.defaultGsonBuilder().create()
    }

    private class TestOperation(
        override val id: String,
        override val data: String,
        override var proximityCheck: ProximityCheck? = null
    ) : IOperation

    @Test
    fun `test authorize request without mobile token data`() {
        val operation = TestOperation("test-id", "test-data")
        val timestamp = ZonedDateTime.now()
        val request = AuthorizeRequestObject(operation, timestamp)
        
        val json = gson.toJson(request)
        
        Assert.assertTrue("JSON should contain id", json.contains("\"id\":\"test-id\""))
        Assert.assertTrue("JSON should contain data", json.contains("\"data\":\"test-data\""))
        Assert.assertFalse("JSON should not contain mobileTokenData when null", json.contains("mobileTokenData"))
    }

    @Test
    fun `test authorize request with mobile token data`() {
        val operation = TestOperation("test-id", "test-data")
        val timestamp = ZonedDateTime.now()
        val mobileTokenData = "custom-mobile-token-data"
        val request = AuthorizeRequestObject(operation, timestamp, mobileTokenData)
        
        val json = gson.toJson(request)
        
        Assert.assertTrue("JSON should contain id", json.contains("\"id\":\"test-id\""))
        Assert.assertTrue("JSON should contain data", json.contains("\"data\":\"test-data\""))
        Assert.assertTrue("JSON should contain mobileTokenData", json.contains("\"mobileTokenData\":\"custom-mobile-token-data\""))
    }

    @Test
    fun `test authorize request primary constructor with mobile token data`() {
        val mobileTokenData = "test-token-data"
        val request = AuthorizeRequestObject("op-id", "op-data", null, mobileTokenData)
        
        val json = gson.toJson(request)
        
        Assert.assertTrue("JSON should contain mobileTokenData", json.contains("\"mobileTokenData\":\"test-token-data\""))
    }
}