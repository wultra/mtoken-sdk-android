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

package com.wultra.android.mtokensdk.api.operation

import android.net.Uri
import com.wultra.android.mtokensdk.operation.PACUtils
import org.junit.Assert
import org.junit.Test

class PACUtilsTests {

    @Test
    fun `test parseQRCode with empty code`() {
        val code = ""
        Assert.assertNull(PACUtils.parseQRCode(code))
    }

    @Test
    fun testQRPACParserWithShortInvalidCode() {
        val code = "abc"
        Assert.assertNull(PACUtils.parseQRCode(code))
    }

    @Test
    fun testQRTPACParserWithValidDeeplinkCode() {
        val code = "scheme://operation?oid=6a1cb007-ff75-4f40-a21b-0b546f0f6cad&potp=73743194"
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertEquals("Parsing of totp", "73743194", parsed?.totp)
        Assert.assertEquals("Parsing of operationId", "6a1cb007-ff75-4f40-a21b-0b546f0f6cad", parsed?.operationId)
    }

    @Test
    fun testQRTPACParserWithInvalidDeeplinkCodeAndBase64OID() {
        val code = "scheme://operation?oid=E/+DRFVmd4iZABEiM0RVZneImQARIjNEVWZ3iJkAESIzRFVmd4iZAA=&totp=12345678"
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull(parsed?.totp)
        Assert.assertNull(parsed?.operationId)
    }

    @Test
    fun testQRTPACParserWithValidDeeplinkCodeAndBase64EncodedOID() {
        val code = "scheme://operation?oid=E%2F%2BDRFVmd4iZABEiM0RVZneImQARIjNEVWZ3iJkAESIzRFVmd4iZAA%3D&totp=12345678"
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertEquals("Parsing of totp", "12345678", parsed?.totp)
        Assert.assertEquals("Parsing of operationId", "E/+DRFVmd4iZABEiM0RVZneImQARIjNEVWZ3iJkAESIzRFVmd4iZAA=", parsed?.operationId)
    }

    fun testQRPACParserWithValidJWT() {
        val code = "eyJhbGciOiJub25lIiwidHlwZSI6IkpXVCJ9.eyJvaWQiOiIzYjllZGZkMi00ZDgyLTQ3N2MtYjRiMy0yMGZhNWM5OWM5OTMiLCJwb3RwIjoiMTQzNTc0NTgifQ=="
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertEquals("Parsing of totp", "14357458", parsed?.totp)
        Assert.assertEquals("Parsing of operationId", "3b9edfd2-4d82-477c-b4b3-20fa5c99c993", parsed?.operationId)
    }

    @Test
    fun testQRPACParserWithValidJWTWithoutPadding() {
        val code = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJvaWQiOiJMRG5JY0NjRGhjRHdHNVNLejhLeWdQeG9PbXh3dHpJc29zMEUrSFBYUHlvIiwicG90cCI6IjU4NTkwMDU5In0"
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertEquals("Parsing of totp", "58590059", parsed?.totp)
        Assert.assertEquals("Parsing of operationId", "LDnIcCcDhcDwG5SKz8KygPxoOmxwtzIsos0E+HPXPyo", parsed?.operationId)
    }

    @Test
    fun testQRPACParserWithInvalidJWT() {
        val code = "eyJhbGciOiJub25lIiwidHlwZSI6IkpXVCJ9eyJvaWQiOiIzYjllZGZkMi00ZDgyLTQ3N2MtYjRiMy0yMGZhNWM5OWM5OTMiLCJwb3RwIjoiMTQzNTc0NTgifQ=="
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull("Parsing of should fail", parsed)
    }

    @Test
    fun testQRPACParserWithInvalidJWT2() {
        val code = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.1eyJvaWQiOiJMRG5JY0NjRGhjRHdHNVNLejhLeWdQeG9PbXh3dHpJc29zMEUrSFBYUHlvIiwicG90cCI6IjU4NTkwMDU5In0"
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull("Parsing of should fail", parsed)
    }

    @Test
    fun testQRPACParserWithInvalidJWT3() {
        val code = ""
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull("Parsing of should fail", parsed)
    }

    @Test
    fun testQRPACParserWithInvalidJWT4() {
        val code = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.1eyJvaWQiOiJMRG5JY0NjR.GhjRHdHNVNLejhLeWdQeG9PbXh3dHpJc29zMEUrSFBYUHlvIiwicG90cCI6IjU4NTkwMDU5In0====="
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull("Parsing of should fail", parsed)
    }

    @Test
    fun testDeeplinkParserWithInvalidPACCode() {
        val code = "operation?oid=df6128fc-ca51-44b7-befa-ca0e1408aa63&potp=56725494"
        Assert.assertNull(PACUtils.parseQRCode(code))
    }

    @Test
    fun testDeeplinkPACParserWithInvalidURL() {
        val url = Uri.parse("scheme://an-invalid-url.com")
        Assert.assertNull(PACUtils.parseDeeplink(url))
    }

    @Test
    fun testDeeplinkParserWithValidURLButInvalidQuery() {
        val url = Uri.parse("scheme://operation?code=abc")
        Assert.assertNull(PACUtils.parseDeeplink(url))
    }

    @Test
    fun testDeeplinkPACParserWithValidJWTCode() {
        val url = Uri.parse("scheme://operation?code=eyJhbGciOiJub25lIiwidHlwZSI6IkpXVCJ9.eyJvaWQiOiIzYjllZGZkMi00ZDgyLTQ3N2MtYjRiMy0yMGZhNWM5OWM5OTMiLCJwb3RwIjoiMTQzNTc0NTgifQ==")
        val parsed = PACUtils.parseDeeplink(url)
        Assert.assertEquals("Parsing of totp failed", "14357458", parsed?.totp)
        Assert.assertEquals("Parsing of operationId failed", "3b9edfd2-4d82-477c-b4b3-20fa5c99c993", parsed?.operationId,)
    }

    @Test
    fun testDeeplinkParserWithValidPACCode() {
        val url = Uri.parse("scheme://operation?oid=df6128fc-ca51-44b7-befa-ca0e1408aa63&potp=56725494")
        val parsed = PACUtils.parseDeeplink(url)
        Assert.assertEquals("Parsing of totp failed", "56725494", parsed?.totp)
        Assert.assertEquals("Parsing of operationId failed", "df6128fc-ca51-44b7-befa-ca0e1408aa63", parsed?.operationId)
    }

    @Test
    fun testDeeplinkPACParserWithValidAnonymousDeeplinkQRCode() {
        val code = "scheme://operation?oid=df6128fc-ca51-44b7-befa-ca0e1408aa63"
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull(parsed?.totp)
        Assert.assertEquals("Parsing of operationId failed", "df6128fc-ca51-44b7-befa-ca0e1408aa63", parsed?.operationId)
    }

    @Test
    fun testDeeplinkPACParserWithAnonymousJWTQRCodeWithOnlyOperationId() {
        val code = "eyJhbGciOiJub25lIiwidHlwZSI6IkpXVCJ9.eyJvaWQiOiI1YWM0YjNlOC05MjZmLTQ1ZjAtYWUyOC1kMWJjN2U2YjA0OTYifQ=="
        val parsed = PACUtils.parseQRCode(code)
        Assert.assertNull(parsed?.totp)
        Assert.assertEquals("Parsing of operationId failed", "5ac4b3e8-926f-45f0-ae28-d1bc7e6b0496", parsed?.operationId)
    }
}
