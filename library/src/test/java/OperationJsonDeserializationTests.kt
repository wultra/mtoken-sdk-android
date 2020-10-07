/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.operation.model.AmountAttribute
import com.wultra.android.mtokensdk.api.operation.model.Attribute
import com.wultra.android.mtokensdk.api.operation.model.OperationListResponse
import com.wultra.android.mtokensdk.api.operation.model.PartyInfoAttribute
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
        gson = builder.create()
        typeAdapter = gson.getAdapter(TypeToken.get(OperationListResponse::class.java))
    }

    @Test
    fun `test wrong response`() {
        val json = "{\"responseObject\": {\"empty\": true},\"status\": \"OK\"}"
        var exception: Throwable? = null
        try {
            val response = typeAdapter.fromJson(json)
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
        Assert.assertEquals("OK", response.status)
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
        Assert.assertEquals("OK", response.status)
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
}