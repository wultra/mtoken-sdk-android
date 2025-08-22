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

        val resultPostApproval = result.ui?.postApprovalScreen as? PostApprovalScreenReview
        val uiPostApproval = ui.postApprovalScreen as? PostApprovalScreenReview

        assertEquals(result.ui?.flipButtons, ui.flipButtons)
        assertEquals(result.ui?.blockApprovalOnCall, ui.blockApprovalOnCall)
        assertEquals(result.ui?.preApprovalScreen?.type, ui.flipButtons)
        assertEquals(resultPostApproval?.heading, uiPostApproval?.heading)
        assertEquals(resultPostApproval?.message, uiPostApproval?.message)
        assertEquals(resultPostApproval?.payload?.attributes, uiPostApproval?.payload?.attributes)
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
                    attributes = listOf<Attribute>(
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

        val resultPostApproval = result.ui?.postApprovalScreen as? PostApprovalScreenReview
        val uiPostApproval = ui.postApprovalScreen as? PostApprovalScreenReview

        assertEquals(result.ui?.flipButtons, ui.flipButtons)
        assertEquals(result.ui?.blockApprovalOnCall, ui.blockApprovalOnCall)
        assertEquals(result.ui?.preApprovalScreen?.type, ui.flipButtons)
        assertEquals(resultPostApproval?.heading, uiPostApproval?.heading)
        assertEquals(resultPostApproval?.message, uiPostApproval?.message)
        assertEquals(resultPostApproval?.payload?.attributes?.get(0)?.type, uiPostApproval?.payload?.attributes?.get(0)?.type)
        assertEquals(resultPostApproval?.payload?.attributes?.get(0)?.label?.id, uiPostApproval?.payload?.attributes?.get(0)?.label?.id)
        assertEquals(resultPostApproval?.payload?.attributes?.get(0)?.label?.value, uiPostApproval?.payload?.attributes?.get(0)?.label?.value)
        assertEquals((resultPostApproval?.payload?.attributes?.get(0) as? NoteAttribute)?.note, (uiPostApproval?.payload?.attributes?.get(0) as? NoteAttribute)?.note)
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
        assertEquals(postApprovalGenericResult.payload["array"], JSONValue.JSONArray(listOf(JSONValue.JSONString("firstElement"), JSONValue.JSONString("secondElement"))))
        assertEquals(postApprovalGenericResult.payload["object"], JSONValue.JSONObject(mapOf("nestedObject" to JSONValue.JSONString("stringValue"))))
    }

    @Test
    fun testPreApprovalScreensWithFallback() {
        val result = prepareResult(preApprovalScreensResponse)
            ?: run { fail("Failed to parse JSON"); return }

        // Top-level flags
        assertEquals(true, result.ui?.flipButtons)
        assertEquals(false, result.ui?.blockApprovalOnCall)

        // New apps: array present with 2 screens
        val screens = result.ui?.preApprovalScreens ?: run {
            fail("preApprovalScreens missing"); return
        }
        assertEquals(2, screens.size)

        // Screen #1 (WARNING)
        val s1 = screens[0]
        assertEquals("id1", s1.id)
        assertEquals(PreApprovalScreen.Type.WARNING, s1.type)
        assertEquals(true, s1.backButton)
        assertEquals("image-label", s1.image)
        assertEquals("Watch out!", s1.heading)
        assertEquals("You may become a victim of an attack.", s1.message)
        assertEquals(true, s1.controls?.flip)
        assertEquals(PreApprovalControls.DeclineType.REJECT, s1.controls?.decline?.type)
        assertEquals("Reject Payment", s1.controls?.decline?.text)
        assertEquals(PreApprovalControls.ApproveType.BUTTON, s1.controls?.approve?.type)
        assertEquals("Approve Payment", s1.controls?.approve?.text)
        assertEquals(10, s1.controls?.approve?.counter)

        // elements
        assertEquals(3, s1.elements?.size)
        s1.elements?.get(0)?.let { e0 ->
            assertEquals(PreApprovalElement.ElementType.ALERT, e0.type)
            assertEquals(PreApprovalElement.AlertStyle.INFO, e0.style)
            assertEquals("Make sure the activation takes place on your device", e0.text)
        } ?: fail("Missing element 0")

        s1.elements?.get(1)?.let { e1 ->
            assertEquals(PreApprovalElement.ElementType.BUTTON, e1.type)
            assertEquals(PreApprovalElement.ButtonAction.PHONE, e1.action)
            assertEquals("Call center", e1.text)
            assertEquals("+42012345678", e1.href)
        } ?: fail("Missing element 1")

        s1.elements?.get(2)?.let { e2 ->
            assertEquals(PreApprovalElement.ElementType.LISTITEM, e2.type)
            assertEquals("icon-label", e2.icon)
            assertEquals("You activate a new app and allow access to your accounts", e2.text)
        } ?: fail("Missing element 2")

        // Screen #2 (QR_SCAN)
        val s2 = screens[1]
        assertEquals("id2", s2.id)
        assertEquals(PreApprovalScreen.Type.QR_SCAN, s2.type)
        assertEquals("Watch out!", s2.heading)
        assertEquals("You may become a victim of an attack.", s2.message)
        assertEquals(null, s2.controls)
        assertEquals(1, s2.elements?.size)
        s2.elements?.first()?.let { e ->
            assertEquals(PreApprovalElement.ElementType.LISTITEM, e.type)
            assertEquals("icon-label", e.icon)
            assertEquals("You activate a new app and allow access to your accounts", e.text)
        } ?: fail("Missing element in screen 2")

        // Old apps: legacy single preApprovalScreen should be present (QR fallback)
        val legacy = result.ui?.preApprovalScreen ?: run {
            fail("legacy preApprovalScreen missing"); return
        }
        assertEquals(PreApprovalScreen.Type.QR_SCAN, legacy.type)
        assertEquals("Watch out!", legacy.heading)
        assertEquals("You may become a victim of an attack.", legacy.message)
        assertEquals(listOf("You activate a new app and allow access to your accounts"), legacy.items)
        assertEquals(null, legacy.approvalType)
    }

    /** Helpers */
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

    private val genericPostApproval: String = """
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
    private val preApprovalScreensResponse: String = """
    {
        "id": "74654880-6db9-4b84-9174-386fc5e7d8ab",
        "name": "authorize_payment_preApproval_multi",
        "data": "A1*A100.00EUR*ICZ3855000000003643174999",
        "status": "PENDING",
        "operationCreated": "2023-04-25T13:09:52+0000",
        "operationExpires": "2023-04-25T13:14:52+0000",
        "ui": {
            "flipButtons": true,
            "blockApprovalOnCall": false,
            "preApprovalScreens": [
                {
                    "id": "id1",
                    "type": "WARNING",
                    "backButton": true,
                    "image": "image-label",
                    "heading": "Watch out!",
                    "message": "You may become a victim of an attack.",
                    "elements": [
                        {
                            "id": "e1",
                            "type": "ALERT",
                            "style": "INFO",
                            "text": "Make sure the activation takes place on your device"
                        },
                        {
                            "id": "e2",
                            "type": "BUTTON",
                            "action": "PHONE",
                            "text": "Call center",
                            "href": "+42012345678"
                        },
                        {
                            "id": "e3",
                            "type": "LISTITEM",
                            "icon": "icon-label",
                            "text": "You activate a new app and allow access to your accounts"
                        }
                    ],
                    "controls": {
                        "flip": true,
                        "decline": {
                            "type": "REJECT",
                            "text": "Reject Payment"
                        },
                        "approve": {
                            "type": "BUTTON",
                            "text": "Approve Payment",
                            "counter": 10
                        }
                    }
                },
                {
                    "id": "id2",
                    "type": "QR_SCAN",
                    "backButton": null,
                    "image": null,
                    "heading": "Watch out!",
                    "message": "You may become a victim of an attack.",
                    "elements": [
                        {
                            "type": "LISTITEM",
                            "icon": "icon-label",
                            "text": "You activate a new app and allow access to your accounts"
                        }
                    ],
                    "controls": null
                }
            ],
            "preApprovalScreen": {
                "type": "QR_SCAN",
                "heading": "Watch out!",
                "message": "You may become a victim of an attack.",
                "items": [
                    "You activate a new app and allow access to your accounts"
                ],
                "approvalType": null
            }
        },
        "allowedSignatureType": {
            "type": "2FA",
            "variants": [
                "possession_knowledge",
                "possession_biometry"
            ]
        },
        "formData": {
            "title": "Payment Approval",
            "message": "Please confirm the payment",
            "attributes": [
                {
                    "type": "AMOUNT",
                    "id": "operation.amount",
                    "label": "Amount",
                    "amount": 100,
                    "currency": "EUR",
                    "amountFormatted": "100,00",
                    "currencyFormatted": "€"
                },
                {
                    "type": "KEY_VALUE",
                    "id": "operation.account",
                    "label": "To Account",
                    "value": "CZ3855000000003643174999"
                }
            ]
        }
    }
"""
}
