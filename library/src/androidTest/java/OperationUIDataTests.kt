/*
 * Copyright 2023 Wultra s.r.o.
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

import com.google.gson.Gson
import com.wultra.android.mtokensdk.api.operation.model.*
import com.wultra.android.mtokensdk.operation.JSONValue
import com.wultra.android.mtokensdk.operation.OperationsUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.junit.Test


class OperationUIDataTests {

    @Test
    fun testPreApprovalWarningResponse() {
        val result = prepareResult(preApprovalResponse)

        if (result != null) {
            val ui = OperationUIData(
                flipButtons = true,
                blockApprovalOnCall = false,
                preApprovalScreen = PreApprovalScreen(
                    type = PreApprovalScreen.Type.WARNING,
                    heading = "Watch out!",
                    message = "You may become a victim of an attack.",
                    items = arrayListOf(
                        "You activate a new app and allow access to your accounts",
                        "Make sure the activation takes place on your device",
                        "If you have been prompted for this operation in connection with a payment, decline it"
                    ),
                    approvalType = PreApprovalScreenConfirmAction.SLIDER
                ),
                postApprovalScreen = null
            )

            assertEquals(result.ui?.flipButtons, ui.flipButtons)
            assertEquals(result.ui?.blockApprovalOnCall, ui.blockApprovalOnCall)
            assertEquals(result.ui?.preApprovalScreen?.type, ui.preApprovalScreen?.type)
            assertEquals(result.ui?.preApprovalScreen?.heading, ui.preApprovalScreen?.heading)
            assertEquals(result.ui?.preApprovalScreen?.message, ui.preApprovalScreen?.message)
            assertEquals(result.ui?.preApprovalScreen?.items, ui.preApprovalScreen?.items)
            assertEquals(result.ui?.preApprovalScreen?.approvalType, ui.preApprovalScreen?.approvalType)

        } else {
            fail("Fail to serialize JSON")
            return
        }
    }

    @Test
    fun testPreApprovalUnknownResponse() {
        val result = prepareResult(preApprovalFutureResponse)

        if (result != null) {
            val ui = OperationUIData(
                flipButtons = true,
                blockApprovalOnCall = false,
                preApprovalScreen = PreApprovalScreen(
                    type = PreApprovalScreen.Type.UNKNOWN,
                    heading = "Future",
                    message = "Future is now, old man.",
                    items = arrayListOf(),
                    approvalType = null
                ),
                postApprovalScreen = null
            )

            assertEquals(result.ui?.preApprovalScreen?.type, ui.preApprovalScreen?.type)
            assertEquals(result.ui?.preApprovalScreen?.heading, ui.preApprovalScreen?.heading)
            assertEquals(result.ui?.preApprovalScreen?.items, ui.preApprovalScreen?.items)
            assertEquals(result.ui?.preApprovalScreen?.approvalType, ui.preApprovalScreen?.approvalType)

        } else {
            fail("Fail to serialize JSON")
            return
        }
    }


    @Test
    fun testPostApprovalResponseRedirect() {
        val result = prepareResult(postApprovalResponseRedirect)
            ?: run {
                fail("Failed to parse JSON data")
                return
            }

        val ui = OperationUIData(
            flipButtons = null,
            blockApprovalOnCall = null,
            preApprovalScreen = null,
            postApprovalScreen = PostApprovalScreenRedirect(
                heading = "Thank you for your order",
                message = "You will be redirected to the merchant application.",
                payload = RedirectPostApprovalScreenPayload(
                    text = "Go to the application",
                    url = "https://www.alza.cz/ubiquiti-unifi-ap-6-pro-d7212937.htm",
                    countdown = 5
                )
            )
        )

        assertEquals(result.ui?.flipButtons, ui.flipButtons)
        assertEquals(result.ui?.blockApprovalOnCall, ui.blockApprovalOnCall)
        assertEquals(result.ui?.preApprovalScreen?.type, ui.flipButtons)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.heading, (ui.postApprovalScreen as? PostApprovalScreenReview)?.heading)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.message, (ui.postApprovalScreen as? PostApprovalScreenReview)?.message)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes, (ui.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes)
    }

    @Test
    fun testPostApprovalResponseReview() {
        val result = prepareResult(postApprovalResponseReview)
            ?: run {
                fail("Failed to parse JSON data")
                return
            }

        val ui = OperationUIData(
            flipButtons = null,
            blockApprovalOnCall = null,
            preApprovalScreen = null,
            postApprovalScreen = PostApprovalScreenReview(
                heading = "Successful",
                message = "The operation was approved.",
                payload = ReviewPostApprovalScreenPayload(
                    listOf<Attribute>(
                        NoteAttribute(
                            note = "myNote",
                            label = Attribute.Label(
                                id = "1",
                                value = "test label"
                            )
                        )
                    )
                )
            )
        )

        assertEquals(result.ui?.flipButtons, ui.flipButtons)
        assertEquals(result.ui?.blockApprovalOnCall, ui.blockApprovalOnCall)
        assertEquals(result.ui?.preApprovalScreen?.type, ui.flipButtons)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.heading, (ui.postApprovalScreen as? PostApprovalScreenReview)?.heading)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.message, (ui.postApprovalScreen as? PostApprovalScreenReview)?.message)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0)?.type, (ui.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0)?.type)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0)?.label?.id, (ui.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0)?.label?.id)
        assertEquals((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0)?.label?.value, (ui.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0)?.label?.value)
        assertEquals(((result.ui?.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0) as? NoteAttribute)?.note, ((ui.postApprovalScreen as? PostApprovalScreenReview)?.payload?.attributes?.get(0) as? NoteAttribute)?.note)
    }


    @Test
    fun testPostApprovalGenericResponse() {
        val result = prepareResult(genericPostApproval)
            ?: run {
                fail("Failed to parse JSON data")
                return
            }

        val postApprovalGenericResult = result.ui?.postApprovalScreen as? PostApprovalScreenGeneric
            ?: run {
                fail("Failed to cast to PostApprovalScreenGeneric")
                return
            }

        val generic = PostApprovalScreenGeneric(
            heading = "Thank you for your order",
            message = "You may close the application now.",
            payload = JSONValue.parse(
                """
            {
                "nestedMessage": "See you next time.",
                "integer": 1,
                "boolean": true,
                "array": ["firstElement", "secondElement"],
                "object": {
                    "nestedObject": "stringValue"
                }
            }
            """
            )
        )

        assertEquals(postApprovalGenericResult.heading, generic.heading)
        assertEquals(postApprovalGenericResult.message, generic.message)
        assertEquals(postApprovalGenericResult.payload, generic.payload)
        assertEquals(postApprovalGenericResult.payload["nestedMessage"], JSONValue.JSONString("See you next time."))
        assertEquals(postApprovalGenericResult.payload["integer"].toString(), JSONValue.JSONNumber(1).toString())
        assertEquals(postApprovalGenericResult.payload["boolean"], JSONValue.JSONBool(true))
        assertEquals(postApprovalGenericResult.payload["array"], JSONValue.JSONArray(listOf(JSONValue.JSONString("firstElement"), JSONValue.JSONString( "secondElement"))))
        assertEquals(postApprovalGenericResult.payload["object"], JSONValue.JSONObject(mapOf("nestedObject" to JSONValue.JSONString("stringValue"))))
    }

    /**
    Helpers
     */
    private val jsonDecoder: Gson = OperationsUtils.defaultGsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()

    private fun prepareResult(response: String): UserOperation? {
        val result = try {
            jsonDecoder.fromJson(response, UserOperation::class.java)
        } catch (e: Exception) {
            null
        }
        return result
    }

    private val preApprovalResponse: String = """
            {
                "id": "74654880-6db9-4b84-9174-386fc5e7d8ab",
                "name": "authorize_payment_preApproval",
                "data": "A1*A100.00EUR*ICZ3855000000003643174999",
                "status": "PENDING",
                "operationCreated": "2023-04-25T13:09:52+0000",
                "operationExpires": "2023-04-25T13:14:52+0000",
                "ui": {
                    "flipButtons": true,
                    "blockApprovalOnCall": false,
                    "preApprovalScreen": {
                        "type": "WARNING",
                        "heading": "Watch out!",
                        "message": "You may become a victim of an attack.",
                        "items": ["You activate a new app and allow access to your accounts", "Make sure the activation takes place on your device", "If you have been prompted for this operation in connection with a payment, decline it"],
                        "approvalType": "SLIDER"
                    }
                },
                "allowedSignatureType": {
                    "type": "2FA",
                    "variants": ["possession_knowledge", "possession_biometry"]
                },
                "formData": {
                    "title": "Payment Approval",
                    "message": "Please confirm the payment",
                    "attributes": [{
                        "type": "AMOUNT",
                        "id": "operation.amount",
                        "label": "Amount",
                        "amount": 100.00,
                        "currency": "EUR",
                        "amountFormatted": "100,00",
                        "currencyFormatted": "€"
                    }, {
                        "type": "KEY_VALUE",
                        "id": "operation.account",
                        "label": "To Account",
                        "value": "CZ3855000000003643174999"
                    }]
                }
            }
    """

    private val preApprovalFutureResponse: String = """
            {
                "id": "74654880-6db9-4b84-9174-386fc5e7d8ab",
                "name": "authorize_payment_preApproval",
                "data": "A1*A100.00EUR*ICZ3855000000003643174999",
                "status": "PENDING",
                "operationCreated": "2023-04-25T13:09:52+0000",
                "operationExpires": "2023-04-25T13:14:52+0000",
                "ui": {
                    "flipButtons": true,
                    "blockApprovalOnCall": false,
                    "preApprovalScreen": {
                        "type": "FUTURE",
                        "heading": "Future",
                        "message": "Future is now, old man.",
                        "items": [] 
                    }
                },
                "allowedSignatureType": {
                    "type": "2FA",
                    "variants": ["possession_knowledge", "possession_biometry"]
                },
                "formData": {
                    "title": "Payment Approval",
                    "message": "Please confirm the payment",
                    "attributes": [{
                        "type": "AMOUNT",
                        "id": "operation.amount",
                        "label": "Amount",
                        "amount": 100.00,
                        "currency": "EUR",
                        "amountFormatted": "100,00",
                        "currencyFormatted": "€"
                    }, {
                        "type": "KEY_VALUE",
                        "id": "operation.account",
                        "label": "To Account",
                        "value": "CZ3855000000003643174999"
                    }]
                }
            }
    """

    private val genericPostApproval: String ="""
    {
        "id": "74654880-6db9-4b84-9174-386fc5e7d8ab",
        "name": "authorize_payment_preApproval",
        "data": "A1*A100.00EUR*ICZ3855000000003643174999",
        "status": "PENDING",
        "operationCreated": "2023-04-25T13:09:52+0000",
        "operationExpires": "2023-04-25T13:14:52+0000",
        "ui": {
            "flipButtons": true,
            "blockApprovalOnCall": false,
            "postApprovalScreen": {
                "type": "GENERIC",
                "heading": "Thank you for your order",
                "message": "You may close the application now.",
                "payload": {
                    "nestedMessage": "See you next time.",
                    "integer": 1,
                    "boolean": true,
                    "array": ["firstElement", "secondElement"],
                    "object": {
                        "nestedObject": "stringValue"
                    }
                }
            }
        },
        "allowedSignatureType": {
            "type": "2FA",
            "variants": ["possession_knowledge", "possession_biometry"]
        },
        "formData": {
            "title": "Payment Approval",
            "message": "Please confirm the payment",
            "attributes": [{
                "type": "AMOUNT",
                "id": "operation.amount",
                "label": "Amount",
                "amount": 100.00,
                "currency": "EUR",
                "amountFormatted": "100,00",
                "currencyFormatted": "€"
            }, {
                "type": "KEY_VALUE",
                "id": "operation.account",
                "label": "To Account",
                "value": "CZ3855000000003643174999"
            }]
        }
    }
    """
    private val postApprovalResponseReview: String = """
    {
        "id": "f68f6e70-a3d8-4616-b138-358e1799599d",
        "name": "authorize_payment_postApproval",
        "data": "A1*A100.00EUR*ICZ3855000000003643174999",
        "status": "PENDING",
        "operationCreated": "2023-04-25T12:29:23+0000",
        "operationExpires": "2023-04-25T12:34:23+0000",
        "ui": {
              "postApprovalScreen": {
                "type": "REVIEW",
                "heading": "Successful",
                "message": "The operation was approved.",
                "payload": {
                  "attributes": [
                    {
                      "type": "NOTE",
                      "id": "1",
                      "label": "test label",
                      "note": "myNote"
                    }
                  ]
                }
              }
            },
        "allowedSignatureType": {
            "type": "2FA",
            "variants": ["possession_knowledge", "possession_biometry"]
        },
        "formData": {
            "title": "Payment Approval",
            "message": "Please confirm the payment",
            "attributes": [{
                "type": "AMOUNT",
                "id": "operation.amount",
                "label": "Amount",
                "amount": 100.00,
                "currency": "EUR",
                "amountFormatted": "100,00",
                "currencyFormatted": "€"
            }, {
                "type": "KEY_VALUE",
                "id": "operation.account",
                "label": "To Account",
                "value": "CZ3855000000003643174999"
            }]
        }
    }
    """

    private val postApprovalResponseRedirect: String = """
    {
        "id": "f68f6e70-a3d8-4616-b138-358e1799599d",
        "name": "authorize_payment_postApproval",
        "data": "A1*A100.00EUR*ICZ3855000000003643174999",
        "status": "PENDING",
        "operationCreated": "2023-04-25T12:29:23+0000",
        "operationExpires": "2023-04-25T12:34:23+0000",
        "ui": {
            "postApprovalScreen": {
                "type": "MERCHANT_REDIRECT",
                "heading": "Thank you for your order",
                "message": "You will be redirected to the merchant application.",
                "payload": {
                    "redirectText": "Go to the application",
                    "redirectUrl": "https://www.alza.cz/ubiquiti-unifi-ap-6-pro-d7212937.htm",
                    "countdown": 5
                }
            }
        },
        "allowedSignatureType": {
            "type": "2FA",
            "variants": ["possession_knowledge", "possession_biometry"]
        },
        "formData": {
            "title": "Payment Approval",
            "message": "Please confirm the payment",
            "attributes": [{
                "type": "AMOUNT",
                "id": "operation.amount",
                "label": "Amount",
                "amount": 100.00,
                "currency": "EUR",
                "amountFormatted": "100,00",
                "currencyFormatted": "€"
            }, {
                "type": "KEY_VALUE",
                "id": "operation.account",
                "label": "To Account",
                "value": "CZ3855000000003643174999"
            }]
        }
    }
    """

}