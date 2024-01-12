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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.powerauth.networking.data.StatusResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.math.BigDecimal

class OperationJsonDeserializationTests {

    private lateinit var gson: Gson
    private lateinit var typeAdapter: TypeAdapter<OperationListResponse>

    @Before
    fun prepareGson() {
        val builder = GsonBuilder()
        builder.registerTypeHierarchyAdapter(Attribute::class.java, AttributeTypeAdapter())
        builder.registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeDeserializer())
        builder.registerTypeAdapter(OperationHistoryEntry::class.java, OperationHistoryEntryDeserializer())
        gson = builder.create()
        typeAdapter = gson.getAdapter(TypeToken.get(OperationListResponse::class.java))
    }

    @Test
    fun `test wrong response`() {
        val json = "{\"responseObject\": {\"empty\": true},\"status\": \"OK\"}"
        var exception: Throwable? = null
        try {
            typeAdapter.fromJson(json)
        } catch (t: Throwable) {
            exception = t
        }
        Assert.assertNotNull(exception)
    }

    @Test
    fun `test empty object`() {
        val json = "{}"
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
    }

    @Test
    fun `test empty list`() {
        val json = "{\"status\":\"OK\",\"responseObject\":[]}"
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
        Assert.assertEquals(StatusResponse.Status.OK, response.status)
        Assert.assertEquals(0, response.responseObject.size)
    }

    @Test
    fun `test real data no attributes`() {
        val json = """{
  "status": "OK",
  "responseObject": [
    {
      "id": "8eebd926-40d4-4214-8208-307f01b0b68f",
      "name": "authorize_payment",
      "data": "A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017",
      "operationCreated": "2018-06-21T13:41:41+0000",
      "operationExpires": "2018-06-21T13:46:45+0000",
      "allowedSignatureType": {
        "type": "2FA",
        "variants": [
          "possession_knowledge",
          "possession_biometry"
        ]
      },
      "formData": {
        "title": "Confirm Payment",
        "message": "Hello,\nplease confirm following payment:",
        "attributes": [
        ]
      }
    }
  ]
}"""
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
        Assert.assertEquals(StatusResponse.Status.OK, response.status)
        Assert.assertEquals(1, response.responseObject.size)
        val operation = response.responseObject[0]
        Assert.assertEquals(0, operation.formData.attributes.size)
    }

    @Test
    fun `test real data`() {
        val json = """{
  "status": "OK",
  "responseObject": [
    {
      "id": "8eebd926-40d4-4214-8208-307f01b0b68f",
      "name": "authorize_payment",
      "data": "A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017",
      "operationCreated": "2018-06-21T13:41:41+0000",
      "operationExpires": "2018-06-21T13:46:45+0000",
      "allowedSignatureType": {
        "type": "2FA",
        "variants": [
          "possession_knowledge",
          "possession_biometry"
        ]
      },
      "formData": {
        "title": "Confirm Payment",
        "message": "Hello,\nplease confirm following payment:",
        "attributes": [
          {
            "type": "AMOUNT",
            "id": "operation.amount",
            "label": "Amount",
            "amount": 100,
            "currency": "CZK"
          },
          {
            "type": "KEY_VALUE",
            "id": "operation.account",
            "label": "To Account",
            "value": "238400856/0300"
          },
          {
            "type": "KEY_VALUE",
            "id": "operation.dueDate",
            "label": "Due Date",
            "value": "Jun 29, 2017"
          },
          {
            "type": "NOTE",
            "id": "operation.note",
            "label": "Note",
            "note": "Utility Bill Payment - 05/2017"
          }
        ]
      }
    }
  ]
}"""
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
        Assert.assertEquals(1, response.responseObject.size)
        val operation = response.responseObject[0]
        Assert.assertEquals(4, operation.formData.attributes.size)
        val amountAttr = operation.formData.attributes[0] as AmountAttribute
        Assert.assertEquals(BigDecimal.valueOf(100), amountAttr.amount)
    }

    @Test
    fun `test real data 2`() {
        val json = """
            {"status":"OK","currentTimestamp":"2023-02-10T12:30:42+0000","responseObject":[{"id":"930febe7-f350-419a-8bc0-c8883e7f71e3","name":"authorize_payment","data":"A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017","operationCreated":"2018-08-08T12:30:42+0000","operationExpires":"2018-08-08T12:35:43+0000","allowedSignatureType":{"type":"2FA","variants":["possession_knowledge", "possession_biometry"]},"formData":{"title":"Potvrzení platby","message":"Dobrý den,prosíme o potvrzení následující platby:","attributes":[{"type":"AMOUNT","id":"operation.amount","label":"Částka","amount":965165234082.23,"currency":"CZK","valueFormatted": "965165234082.23 CZK"},{"type":"KEY_VALUE","id":"operation.account","label":"Na účet","value":"238400856/0300"},{"type":"KEY_VALUE","id":"operation.dueDate","label":"Datum splatnosti","value":"29.6.2017"},{"type":"NOTE","id":"operation.note","label":"Poznámka","note":"Utility Bill Payment - 05/2017"},{"type":"PARTY_INFO","id":"operation.partyInfo","label":"Application","partyInfo":{"logoUrl":"http://whywander.com/wp-content/uploads/2017/05/prague_hero-100x100.jpg","name":"Tesco","description":"Objevte více příběhů psaných s chutí","websiteUrl":"https://itesco.cz/hello/vse-o-jidle/pribehy-psane-s-chuti/clanek/tomovy-burgery-pro-zapalene-fanousky/15012"}},{ "type": "AMOUNT_CONVERSION", "id": "operation.conversion", "label": "Conversion", "dynamic": true, "sourceAmount": 1.26, "sourceCurrency": "ETC", "sourceAmountFormatted": "1.26", "sourceCurrencyFormatted": "ETC", "sourceValueFormatted": "1.26 ETC", "targetAmount": 1710.98, "targetCurrency": "USD", "targetAmountFormatted": "1,710.98", "targetCurrencyFormatted": "USD", "targetValueFormatted": "1,710.98 USD"},{ "type": "IMAGE", "id": "operation.image", "label": "Image", "thumbnailUrl": "https://example.com/123_thumb.jpeg", "originalUrl": "https://example.com/123.jpeg" },{ "type": "IMAGE", "id": "operation.image", "label": "Image", "thumbnailUrl": "https://example.com/123_thumb.jpeg" },{ "type": "IMAGE", "id": "operation.image", "label": "Image", "thumbnailUrl": "https://example.com/123_thumb.jpeg", "originalUrl": 12345 }]}},{"id":"930febe7-f350-419a-8bc0-c8883e7f71e3","name":"authorize_payment","data":"A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017","operationCreated":"2018-08-08T12:30:42+0000","operationExpires":"2018-08-08T12:35:43+0000","allowedSignatureType":{"type":"1FA","variants":["possession_knowledge"]},"formData":{"title":"Potvrzení platby","message":"Dobrý den,prosíme o potvrzení následující platby:","attributes":[{"type":"AMOUNT","id":"operation.amount","label":"Částka","amount":100,"currency":"CZK"},{"type":"KEY_VALUE","id":"operation.account","label":"Na účet","value":"238400856/0300"},{"type":"KEY_VALUE","id":"operation.dueDate","label":"Datum splatnosti","value":"29.6.2017"},{"type":"NOTE","id":"operation.note","label":"Poznámka","note":"Utility Bill Payment - 05/2017"}]}}]}
        """.trimIndent()
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
        Assert.assertEquals(1676032242000, response.currentTimestamp?.toInstant()?.toEpochMilli())
        Assert.assertEquals(2, response.responseObject.size)
        val operation = response.responseObject[0]
        Assert.assertEquals(9, operation.formData.attributes.size)

        val amountAttr = operation.formData.attributes[0] as AmountAttribute
        Assert.assertEquals(Attribute.Type.AMOUNT, amountAttr.type)
        Assert.assertEquals("operation.amount", amountAttr.label.id)
        Assert.assertEquals("Částka", amountAttr.label.value)
        Assert.assertEquals(BigDecimal(965165234082.23), amountAttr.amount)
        Assert.assertEquals("CZK", amountAttr.currency)
        Assert.assertEquals("965165234082.23 CZK", amountAttr.valueFormatted)
        // old implementation was null, now formatted values are not nullable and are made from amount and currency
        Assert.assertNotNull(amountAttr.amountFormatted)
        Assert.assertNotNull(amountAttr.currencyFormatted)

        val kva = operation.formData.attributes[1] as KeyValueAttribute
        Assert.assertEquals(Attribute.Type.KEY_VALUE, kva.type)
        Assert.assertEquals("operation.account", kva.label.id)
        Assert.assertEquals("Na účet", kva.label.value)
        Assert.assertEquals("238400856/0300", kva.value)

        val na = operation.formData.attributes[3] as NoteAttribute
        Assert.assertEquals(Attribute.Type.NOTE, na.type)
        Assert.assertEquals("operation.note", na.label.id)
        Assert.assertEquals("Poznámka", na.label.value)
        Assert.assertEquals("Utility Bill Payment - 05/2017", na.note)

        val pia = operation.formData.attributes[4] as PartyInfoAttribute
        Assert.assertEquals(Attribute.Type.PARTY_INFO, pia.type)
        Assert.assertEquals("operation.partyInfo", pia.label.id)
        Assert.assertEquals("Application", pia.label.value)
        Assert.assertNotNull(pia.partyInfo.websiteUrl)

        val ca = operation.formData.attributes[5] as ConversionAttribute
        Assert.assertEquals(Attribute.Type.AMOUNT_CONVERSION, ca.type)
        Assert.assertEquals("operation.conversion", ca.label.id)
        Assert.assertEquals("Conversion", ca.label.value)
        Assert.assertTrue(ca.dynamic)
        Assert.assertEquals(ca.source.amount, BigDecimal(1.26))
        Assert.assertEquals(ca.source.currency, "ETC")
        Assert.assertEquals(ca.source.amountFormatted, "1.26")
        Assert.assertEquals(ca.source.currencyFormatted, "ETC")
        Assert.assertEquals(ca.source.valueFormatted, "1.26 ETC")
        Assert.assertEquals(ca.target.amount, BigDecimal(1710.98))
        Assert.assertEquals(ca.target.currency, "USD")
        Assert.assertEquals(ca.target.amountFormatted, "1,710.98")
        Assert.assertEquals(ca.target.currencyFormatted, "USD")
        Assert.assertEquals(ca.target.valueFormatted, "1,710.98 USD")

        val ia = operation.formData.attributes[6] as ImageAttribute
        Assert.assertEquals(Attribute.Type.IMAGE, ia.type)
        Assert.assertEquals("operation.image", ia.label.id)
        Assert.assertEquals("Image", ia.label.value)
        Assert.assertEquals("https://example.com/123_thumb.jpeg", ia.thumbnailUrl)
        Assert.assertEquals("https://example.com/123.jpeg", ia.originalUrl)

        val ia2 = operation.formData.attributes[7] as ImageAttribute
        Assert.assertEquals(Attribute.Type.IMAGE, ia2.type)
        Assert.assertEquals("operation.image", ia2.label.id)
        Assert.assertEquals("Image", ia2.label.value)
        Assert.assertEquals("https://example.com/123_thumb.jpeg", ia2.thumbnailUrl)
        Assert.assertEquals(null, ia2.originalUrl)

        val ia3 = operation.formData.attributes[8] as ImageAttribute
        Assert.assertEquals(Attribute.Type.IMAGE, ia3.type)
        Assert.assertEquals("operation.image", ia3.label.id)
        Assert.assertEquals("Image", ia3.label.value)
        Assert.assertEquals("https://example.com/123_thumb.jpeg", ia3.thumbnailUrl)
        // here we're testing if a wrong type (int) was parsed to null
        Assert.assertEquals(null, ia3.originalUrl)
    }

    @Test
    fun `test Amount & Conversion Attributes response with only amount and currency - legacy backend`() {
        val json = """{"status":"OK", "currentTimestamp":"2023-02-10T12:30:42+0000", "responseObject":[{"id":"930febe7-f350-419a-8bc0-c8883e7f71e3", "name":"authorize_payment", "data":"A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017", "operationCreated":"2018-08-08T12:30:42+0000", "operationExpires":"2018-08-08T12:35:43+0000", "allowedSignatureType": {"type":"2FA", "variants": ["possession_knowledge", "possession_biometry"]}, "formData": {"title":"Potvrzení platby", "message":"Dobrý den,prosíme o potvrzení následující platby:", "attributes": [{"type":"AMOUNT", "id":"operation.amount", "label":"Částka", "amount":965165234082.23, "currency":"CZK"}, { "type": "AMOUNT_CONVERSION", "id": "operation.conversion", "label": "Conversion", "dynamic": true, "sourceAmount": 1.26, "sourceCurrency": "ETC", "targetAmount": 1710.98, "targetCurrency": "USD"}]}}]}
        """.trimIndent()

        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)

        val amountAttr = response.responseObject[0].formData.attributes[0] as AmountAttribute
        Assert.assertEquals(BigDecimal(965165234082.23), amountAttr.amount)
        Assert.assertEquals("CZK", amountAttr.currency)
        Assert.assertEquals("965165234082.23", amountAttr.amountFormatted)
        Assert.assertEquals("CZK", amountAttr.currencyFormatted)

        val conversionAttr = response.responseObject[0].formData.attributes[1] as ConversionAttribute
        Assert.assertEquals(BigDecimal(1.26), conversionAttr.source.amount)
        Assert.assertEquals("ETC", conversionAttr.source.currency)
        Assert.assertEquals(BigDecimal(1710.98), conversionAttr.target.amount)
        Assert.assertEquals("USD", conversionAttr.target.currency)

        Assert.assertEquals("1.26", conversionAttr.source.amountFormatted)
        Assert.assertEquals("ETC", conversionAttr.source.currencyFormatted)
        Assert.assertEquals("1710.98", conversionAttr.target.amountFormatted)
        Assert.assertEquals("USD", conversionAttr.target.currencyFormatted)
    }

    @Test
    fun `test Amount & Conversion Attributes response with only amountFormatted and currencyFormatted`() {
        val json = """{"status":"OK", "currentTimestamp":"2023-02-10T12:30:42+0000", "responseObject":[{"id":"930febe7-f350-419a-8bc0-c8883e7f71e3", "name":"authorize_payment", "data":"A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017", "operationCreated":"2018-08-08T12:30:42+0000", "operationExpires":"2018-08-08T12:35:43+0000", "allowedSignatureType": {"type":"2FA", "variants": ["possession_knowledge", "possession_biometry"]}, "formData": {"title":"Potvrzení platby", "message":"Dobrý den,prosíme o potvrzení následující platby:", "attributes": [{"type":"AMOUNT", "id":"operation.amount", "label":"Částka", "amountFormatted":"965165234082.23", "currencyFormatted":"CZK"}, { "type": "AMOUNT_CONVERSION", "id": "operation.conversion", "label": "Conversion", "dynamic": true, "sourceAmountFormatted": "1.26", "sourceCurrencyFormatted": "ETC", "targetAmountFormatted": "1710.98", "targetCurrencyFormatted": "USD"}]}}]}
        """.trimIndent()

        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)

        val amountAttr = response.responseObject[0].formData.attributes[0] as AmountAttribute
        Assert.assertNull(amountAttr.amount)
        Assert.assertNull(amountAttr.currency)
        Assert.assertEquals("965165234082.23", amountAttr.amountFormatted)
        Assert.assertEquals("CZK", amountAttr.currencyFormatted)

        val conversionAttr = response.responseObject[0].formData.attributes[1] as ConversionAttribute
        Assert.assertNull(conversionAttr.source.amount)
        Assert.assertNull(conversionAttr.source.currency)
        Assert.assertNull(conversionAttr.target.amount)
        Assert.assertNull(conversionAttr.target.currency)

        Assert.assertEquals("1.26", conversionAttr.source.amountFormatted)
        Assert.assertEquals("ETC", conversionAttr.source.currencyFormatted)
        Assert.assertEquals("1710.98", conversionAttr.target.amountFormatted)
        Assert.assertEquals("USD", conversionAttr.target.currencyFormatted)
    }

    @Test
    fun `test unknown attribute`() {
        val json = """{"status":"OK","responseObject":[{"id":"930febe7-f350-419a-8bc0-c8883e7f71e3","name":"authorize_payment","data":"A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017","operationCreated":"2018-08-08T12:30:42+0000","operationExpires":"2018-08-08T12:35:43+0000","allowedSignatureType":{"type":"2FA","variants":["possession_knowledge", "possession_biometry"]},"formData":{"title":"Potvrzení platby","message":"Dobrý den,prosíme o potvrzení následující platby:","attributes":[{"type":"THIS_IS_FAKE_ATTR","id":"operation.amount","label":"Částka","amount":965165234082.23,"currency":"CZK","valueFormatted":"965165234082.23 CZK"},{"type":"KEY_VALUE","id":"operation.account","label":"Na účet","value":"238400856/0300"}]}}]}"""
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
        Assert.assertEquals(1, response.responseObject.size)
        Assert.assertEquals(2, response.responseObject[0].formData.attributes.size)
        Assert.assertEquals(Attribute.Type.UNKNOWN, response.responseObject[0].formData.attributes[0].type)
    }

    @Test
    fun `test real data PARTY_INFO`() {
        val json = """{
  "status": "OK",
  "responseObject": [
    {
      "id": "1f1c14fc-6ebd-48c4-be62-6fc00cbb86ea",
      "name": "authorize_payment",
      "data": "A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017",
      "operationCreated": "2018-10-31T10:19:31+0000",
      "operationExpires": "2018-10-31T10:24:47+0000",
      "allowedSignatureType": {
        "type": "2FA",
        "variants": [
          "possession_knowledge",
          "possession_biometry"
        ]
      },
      "formData": {
        "title": "Potvrzení platby",
        "message": "Dobrý den,\nprosíme o potvrzení následující platby:",
        "attributes": [
          {
            "type": "PARTY_INFO",
            "id": "operation.partyInfo",
            "label": "Aplikace",
            "partyInfo": {
              "logoUrl": "https://itesco.cz/img/logo/logo.svg",
              "name": "Tesco",
              "description": "Find out more about Tesco...",
              "websiteUrl": "https://itesco.cz/hello"
            }
          }
        ]
      }
    }
  ]
}"""
        val response = typeAdapter.fromJson(json)
        Assert.assertNotNull(response)
        Assert.assertEquals(1, response.responseObject.size)
        val operation = response.responseObject[0]
        Assert.assertEquals(1, operation.formData.attributes.size)
        val partyInfoAttribute = operation.formData.attributes[0] as PartyInfoAttribute
        val partyInfo = partyInfoAttribute.partyInfo
        Assert.assertNotNull(partyInfo)
        Assert.assertEquals("Tesco", partyInfo.name)
    }

    @Test
    fun `test history response`() {
        val json = """{ "status":"OK", "responseObject":[ { "id":"0775afb2-4f06-4ed9-b990-a35bab4cac3b", "name":"login-tpp", "data":"A2*R666*R123", "status":"PENDING", "operationCreated":"2021-08-09T15:32:24+0000", "operationExpires":"2021-08-09T15:37:24+0000", "allowedSignatureType":{ "type":"2FA", "variants":[ "possession_knowledge", "possession_biometry" ] }, "formData":{ "title":"Login Approval", "message":"Are you logging in to the third party application?", "attributes":[ { "type":"KEY_VALUE", "id":"party.name", "label":"Third Party App", "value":"Datová schránka" }, { "type":"KEY_VALUE", "id":"party.id", "label":"Application ID", "value":"666" }, { "type":"KEY_VALUE", "id":"session.id", "label":"Session ID", "value":"123" }, { "type":"KEY_VALUE", "id":"session.ip-address", "label":"IP Address", "value":"192.168.0.1" } ] } }, { "id":"5bbe1d48-d2f0-43fb-8612-75917a9761fb", "name":"login-tpp", "data":"A2*R666*R123", "status":"REJECTED", "operationCreated":"2021-08-09T15:32:15+0000", "operationExpires":"2021-08-09T15:37:15+0000", "allowedSignatureType":{ "type":"2FA", "variants":[ "possession_knowledge", "possession_biometry" ] }, "formData":{ "title":"Login Approval", "message":"Are you logging in to the third party application?", "attributes":[ { "type":"KEY_VALUE", "id":"party.name", "label":"Third Party App", "value":"Datová schránka" }, { "type":"KEY_VALUE", "id":"party.id", "label":"Application ID", "value":"666" }, { "type":"KEY_VALUE", "id":"session.id", "label":"Session ID", "value":"123" }, { "type":"KEY_VALUE", "id":"session.ip-address", "label":"IP Address", "value":"192.168.0.1" } ] } }, { "id":"8bbff7b6-03c4-470c-9320-4660c3bf1f01", "name":"login-tpp", "data":"A2*R666*R123", "status":"APPROVED", "operationCreated":"2021-08-09T15:31:55+0000", "operationExpires":"2021-08-09T15:36:55+0000", "allowedSignatureType":{ "type":"2FA", "variants":[ "possession_knowledge", "possession_biometry" ] }, "formData":{ "title":"Login Approval", "message":"Are you logging in to the third party application?", "attributes":[ { "type":"KEY_VALUE", "id":"party.name", "label":"Third Party App", "value":"Datová schránka" }, { "type":"KEY_VALUE", "id":"party.id", "label":"Application ID", "value":"666" }, { "type":"KEY_VALUE", "id":"session.id", "label":"Session ID", "value":"123" }, { "type":"KEY_VALUE", "id":"session.ip-address", "label":"IP Address", "value":"192.168.0.1" } ] } }, { "id":"8bbff7b6-03c4-470c-9320-4660c3bf1f01", "name":"login-tpp", "data":"A2*R666*R123", "status":"CANCELED", "operationCreated":"2021-08-09T15:31:55+0000", "operationExpires":"2021-08-09T15:36:55+0000", "allowedSignatureType":{ "type":"2FA", "variants":[ "possession_knowledge", "possession_biometry" ] }, "formData":{ "title":"Login Approval", "message":"Are you logging in to the third party application?", "attributes":[ { "type":"KEY_VALUE", "id":"party.name", "label":"Third Party App", "value":"Datová schránka" }, { "type":"KEY_VALUE", "id":"party.id", "label":"Application ID", "value":"666" }, { "type":"KEY_VALUE", "id":"session.id", "label":"Session ID", "value":"123" }, { "type":"KEY_VALUE", "id":"session.ip-address", "label":"IP Address", "value":"192.168.0.1" } ] } }, { "id":"8bbff7b6-03c4-470c-9320-4660c3bf1f01", "name":"login-tpp", "data":"A2*R666*R123", "status":"EXPIRED", "operationCreated":"2021-08-09T15:31:55+0000", "operationExpires":"2021-08-09T15:36:55+0000", "allowedSignatureType":{ "type":"2FA", "variants":[ "possession_knowledge", "possession_biometry" ] }, "formData":{ "title":"Login Approval", "message":"Are you logging in to the third party application?", "attributes":[ { "type":"KEY_VALUE", "id":"party.name", "label":"Third Party App", "value":"Datová schránka" }, { "type":"KEY_VALUE", "id":"party.id", "label":"Application ID", "value":"666" }, { "type":"KEY_VALUE", "id":"session.id", "label":"Session ID", "value":"123" }, { "type":"KEY_VALUE", "id":"session.ip-address", "label":"IP Address", "value":"192.168.0.1" } ] } }, { "id":"8bbff7b6-03c4-470c-9320-4660c3bf1f01", "name":"login-tpp", "data":"A2*R666*R123", "status":"FAILED", "operationCreated":"2021-08-09T15:31:55+0000", "operationExpires":"2021-08-09T15:36:55+0000", "allowedSignatureType":{ "type":"2FA", "variants":[ "possession_knowledge", "possession_biometry" ] }, "formData":{ "title":"Login Approval", "message":"Are you logging in to the third party application?", "attributes":[ { "type":"KEY_VALUE", "id":"party.name", "label":"Third Party App", "value":"Datová schránka" }, { "type":"KEY_VALUE", "id":"party.id", "label":"Application ID", "value":"666" }, { "type":"KEY_VALUE", "id":"session.id", "label":"Session ID", "value":"123" }, { "type":"KEY_VALUE", "id":"session.ip-address", "label":"IP Address", "value":"192.168.0.1" } ] } } ] }"""
        val resp = gson.fromJson(json, OperationHistoryResponse::class.java)

        OperationHistoryEntryStatus.values().forEach { status ->
            assert(resp.responseObject.find { it.status == status } != null)
        }
    }
}
