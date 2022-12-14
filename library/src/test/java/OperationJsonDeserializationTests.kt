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
        val json = """{
  "status": "OK",
  "responseObject": [
    {
      "id": "8694f225-c87d-42d5-b599-eb4914b29f5c",
      "name": "authorize_payment",
      "data": "A1*A100CZK*Q238400856/0300**D20170629*NUtility Bill Payment - 05/2017",
      "operationCreated": "2018-06-22T15:41:00+0000",
      "operationExpires": "2018-06-22T15:46:10+0000",
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