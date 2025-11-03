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
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalControls
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElement
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementAlert
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementButton
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalElementListItem
import com.wultra.android.mtokensdk.api.operation.model.preapproval.PreApprovalScreen
import com.wultra.android.mtokensdk.operation.JSONValue
import com.wultra.android.mtokensdk.operation.OperationsUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.fail
import org.junit.Test

class OperationUIDataTests {

    @Test
    fun testPreApprovalWarningResponseLegacy() {
        val result = prepareResult(preApprovalResponse)
        val ui: OperationUIData? = result?.ui

        val expectedUI = OperationUIData(
            flipButtons = true,
            blockApprovalOnCall = false,
            preApprovalScreens = listOf(
                PreApprovalScreen(
                    type = PreApprovalScreen.Type.WARNING,
                    heading = "Watch out!",
                    message = "You may become a victim of an attack.",
                    id = null,
                    backButton = null,
                    image = null,
                    elements = listOf(
                        PreApprovalElementListItem(text = "You activate a new app and allow access to your accounts"),
                        PreApprovalElementListItem(text = "Make sure the activation takes place on your device"),
                        PreApprovalElementListItem(text = "If you have been prompted for this operation in connection with a payment, decline it")
                    ),
                    controls = PreApprovalControls(approve = PreApprovalControls.Approve(PreApprovalControls.ApproveType.SLIDER))
                )
            ),
            postApprovalScreen = null
        )

        val screens = ui?.preApprovalScreens
        assertNotNull("preApprovalScreens should not be null", screens)
        assertEquals(expectedUI.flipButtons, ui?.flipButtons)
        assertEquals(expectedUI.blockApprovalOnCall, ui?.blockApprovalOnCall)

        // one translated screen
        assertEquals(1, screens?.size)
        val s0 = screens!![0]

        assertEquals(expectedUI.preApprovalScreens?.first()?.type, s0.type)
        assertEquals(expectedUI.preApprovalScreens?.first()?.heading, s0.heading)
        assertEquals(expectedUI.preApprovalScreens?.first()?.message, s0.message)

        // elements: 3 list items with same texts
        val elements = s0.elements ?: error("elements should not be null")
        assertEquals(3, elements.size)
        val texts = elements.map {
            assertEquals(true, it is PreApprovalElementListItem)
            (it as PreApprovalElementListItem).text
        }
        assertEquals(
            listOf(
                "You activate a new app and allow access to your accounts",
                "Make sure the activation takes place on your device",
                "If you have been prompted for this operation in connection with a payment, decline it"
            ),
            texts
        )

        // controls.approve == SLIDER
        val controls = s0.controls ?: error("controls should not be null")
        val approve = controls.approve ?: error("controls.approve should not be null")
        assertEquals(PreApprovalControls.ApproveType.SLIDER, approve.type)
    }

    @Test
    fun testPreApprovalUnknownResponse() {
        val result = prepareResult(preApprovalFutureResponse)
            ?: run { fail("Fail to serialize JSON"); return }

        // Expected UI
        val expectedUi = OperationUIData(
            flipButtons = true,
            blockApprovalOnCall = false,
            preApprovalScreens = listOf(
                PreApprovalScreen(
                    type = PreApprovalScreen.Type.UNKNOWN,
                    heading = "Future",
                    message = "Future is now, old man."
                )
            ),
            postApprovalScreen = null
        )

        val screens = result.ui?.preApprovalScreens
        assertNotNull("preApprovalScreens should not be null", screens)
        assertEquals(1, screens!!.size)

        // Compare selected fields to expected
        assertEquals(expectedUi.preApprovalScreens?.get(0)?.type, screens[0].type)
        assertEquals(expectedUi.preApprovalScreens?.get(0)?.heading, screens[0].heading)
        assertEquals(expectedUi.preApprovalScreens?.get(0)?.message, screens[0].message)
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
            preApprovalScreens = null,
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

        assertEquals(null, result.ui?.flipButtons)
        assertEquals(null, result.ui?.blockApprovalOnCall)
        assertEquals(null, result.ui?.preApprovalScreens)
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
            preApprovalScreens = null,
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

        assertEquals(null, result.ui?.flipButtons)
        assertEquals(null, result.ui?.blockApprovalOnCall)
        assertEquals(null, result.ui?.preApprovalScreens)
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
    fun testPreApprovalScreensResponseWithPreApprovalIgnoredLegacy() {
        val result = prepareResult(preApprovalScreensResponse)
        assertNotNull("Failed to parse JSON data", result)

        // New apps: array should be present with 2 screens
        val screens = result!!.ui?.preApprovalScreens
        assertNotNull("preApprovalScreens missing", screens)
        assertEquals("preApprovalScreens has wrong count", 2, screens!!.size)

        // Screen1 (WARNING)
        val s1 = screens[0]
        assertEquals(PreApprovalScreen.Type.WARNING, s1.type)
        assertEquals("id1", s1.id)
        assertEquals(true, s1.backButton)
        assertEquals("image-label", s1.image)
        assertEquals("Watch out!", s1.heading)
        assertEquals("You may become a victim of an attack.", s1.message)

        // controls
        val c1 = s1.controls
        assertNotNull(c1)
        assertEquals(true, c1!!.flip)
        assertEquals(PreApprovalControls.DeclineType.REJECT, c1.decline?.type)
        assertEquals("Reject Payment", c1.decline?.text)
        assertEquals(PreApprovalControls.ApproveType.BUTTON, c1.approve?.type)
        assertEquals("Approve Payment", c1.approve?.text)
        assertEquals(10, c1.approve?.counter)

        // elements
        val e1 = s1.elements
        assertNotNull(e1)
        assertEquals(3, e1!!.size)

        // Alert element (first)
        val first = e1.first()
        assertEquals(true, first is PreApprovalElementAlert)
        (first as PreApprovalElementAlert).let { alert ->
            assertEquals(PreApprovalElement.Type.ALERT, alert.type)
            assertEquals(PreApprovalElement.Style.INFO, alert.style)
            assertEquals("Make sure the activation takes place on your device", alert.text)
        }

        // Button element (second)
        val second = e1[1]
        assertEquals(true, second is PreApprovalElementButton)
        (second as PreApprovalElementButton).let { btn ->
            assertEquals(PreApprovalElement.Type.BUTTON, btn.type)
            assertEquals("e2", btn.id)
            assertEquals(PreApprovalElementButton.ButtonAction.PHONE, btn.action)
            assertEquals("REJECT", btn.actionSettings)
            assertEquals("+42012345678", btn.href)
            assertEquals("Call center", btn.text)
        }

        // List item element (third)
        val third = e1[2]
        assertEquals(true, third is PreApprovalElementListItem)
        (third as PreApprovalElementListItem).let { item ->
            assertEquals(PreApprovalElement.Type.LIST_ITEM, item.type)
            assertEquals("e3", item.id)
            assertEquals("icon-label", item.icon)
            assertEquals("You activate a new app and allow access to your accounts", item.text)
        }

        // Screen2 (QR_SCAN)
        val s2 = screens[1]
        assertEquals("id2", s2.id)
        assertEquals(PreApprovalScreen.Type.QR_SCAN, s2.type)
        assertEquals(null, s2.backButton)
        assertEquals(null, s2.image)
        assertEquals("Watch out!", s2.heading)
        assertEquals("You may become a victim of an attack.", s2.message)
        assertEquals(null, s2.controls)
        assertEquals(null, s2.elements)

        // Sanity: top-level flags still parsed
        assertEquals(true, result.ui?.flipButtons)
        assertEquals(false, result.ui?.blockApprovalOnCall)
    }

    @Test
    fun testLegacyPreApproval() {
        val result = prepareResult(legacyPreApproval())
            ?: run { fail("Failed to parse JSON data"); return }

        val first = result.ui?.preApprovalScreens?.firstOrNull()
            ?: run { fail("preApprovalScreens missing"); return }

        assertEquals(PreApprovalScreen.Type.WARNING, first.type)
        val elements = first.elements ?: emptyList()
        val texts = elements.map {
            assert(it is PreApprovalElementListItem)
            (it as PreApprovalElementListItem).text
        }
        assertEquals(listOf("A", "B", "C"), texts)

        val approve = first.controls?.approve ?: run { fail("controls.approve missing"); return }
        assertEquals(PreApprovalControls.ApproveType.SLIDER, approve.type)
        assertEquals(null, approve.text)
        assertEquals(null, approve.counter)
    }

    @Test
    fun testLegacyEmptyItemsBecomeNull() {
        val result = prepareResult(preApprovalFutureResponse)
            ?: run { fail("Failed to parse JSON data"); return }

        val first = result.ui?.preApprovalScreens?.firstOrNull()
            ?: run { fail("preApprovalScreens missing"); return }

        // FUTURE → UNKNOWN (forward-compat), empty items → null
        assertEquals(PreApprovalScreen.Type.UNKNOWN, first.type)
        assertEquals(null, first.elements)
    }

    @Test
    fun testSingularIsWrappedIntoPlural() {
        val result = prepareResult(preApprovalResponse)
            ?: run { fail("Failed to parse JSON data"); return }

        val screens = result.ui?.preApprovalScreens
        assertNotNull(screens)
        assertEquals(1, screens!!.size)
    }

    @Test
    fun testUnknownScreenTypeForwardCompat() {
        val json = """
        {
          "id":"1","name":"n","data":"d","status":"PENDING",
          "operationCreated":"2023-04-25T13:09:52+0000",
          "operationExpires":"2023-04-25T13:14:52+0000",
          "ui":{"preApprovalScreen":{"type":"FUTURE","heading":"Future","message":"Future is now, old man."}},
          "allowedSignatureType":{"type":"2FA","variants":[]},
          "formData":{"title":"t","message":"m","attributes":[]}
        }
    """
        val r = prepareResult(json) ?: run { fail("parse fail"); return }
        val s = r.ui?.preApprovalScreens?.firstOrNull() ?: run { fail("no screen"); return }
        assertEquals(PreApprovalScreen.Type.UNKNOWN, s.type)
        assertEquals("Future", s.heading)
        assertEquals("Future is now, old man.", s.message)
    }

    @Test
    fun testUnknownElementTypeForwardCompat() {
        val json = """
        {
          "id":"1","name":"n","data":"d","status":"PENDING",
          "operationCreated":"2023-04-25T13:09:52+0000",
          "operationExpires":"2023-04-25T13:14:52+0000",
          "ui":{
            "preApprovalScreen":{
              "type":"WARNING","heading":"H","message":"M",
              "elements":[ {"type":"TOTALLY_NEW","text":"new-kind"} ]
            }
          },
          "allowedSignatureType":{"type":"2FA","variants":[]},
          "formData":{"title":"t","message":"m","attributes":[]}
        }
    """
        val r = prepareResult(json) ?: run { fail("parse fail"); return }
        val s = r.ui?.preApprovalScreens?.firstOrNull() ?: run { fail("no screen"); return }
        val e = s.elements?.firstOrNull() ?: run { fail("no elements"); return }
        assertEquals(PreApprovalElement.Type.UNKNOWN, e.type)
        assertEquals("new-kind", e.text)
    }

    @Test
    fun testControlsApproveAndDeclineVariants() {
        val json = """
        {
          "id":"1","name":"n","data":"d","status":"PENDING",
          "operationCreated":"2023-04-25T13:09:52+0000",
          "operationExpires":"2023-04-25T13:14:52+0000",
          "ui":{
            "preApprovalScreen":{
              "type":"INFO","heading":"H","message":"M",
              "controls":{
                "flip":true,
                "axis":"HORIZONTAL",
                "decline":{"type":"BACK","text":"Back"},
                "approve":{"type":"BUTTON","text":"Confirm","counter":3}
              }
            }
          },
          "allowedSignatureType":{"type":"2FA","variants":[]},
          "formData":{"title":"t","message":"m","attributes":[]}
        }
    """
        val r = prepareResult(json) ?: run { fail("parse fail"); return }
        val s = r.ui?.preApprovalScreens?.firstOrNull() ?: run { fail("no screen"); return }
        val c = s.controls ?: run { fail("controls missing"); return }
        assertEquals(true, c.flip)
        assertEquals(PreApprovalControls.ButtonAxis.HORIZONTAL, c.axis)
        assertEquals(PreApprovalControls.DeclineType.BACK, c.decline?.type)
        assertEquals("Back", c.decline?.text)
        assertEquals(PreApprovalControls.ApproveType.BUTTON, c.approve?.type)
        assertEquals("Confirm", c.approve?.text)
        assertEquals(3, c.approve?.counter)
    }

    @Test
    fun testLegacyEmptyItemsBecomeNilElements() {
        val result = prepareResult(preApprovalFutureResponse)
            ?: run { fail("Failed to parse JSON data"); return }

        val first = result.ui?.preApprovalScreens?.firstOrNull()
            ?: run { fail("preApprovalScreens missing"); return }

        // FUTURE → UNKNOWN
        assertEquals(PreApprovalScreen.Type.UNKNOWN, first.type)
        assertEquals(null, first.elements) // empty items → nil
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
                            "actionSettings": "REJECT",
                            "text": "Call center",
                            "href": "+42012345678"
                        },
                        {
                            "id": "e3",
                            "type": "LIST_ITEM",
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
                    "message": "You may become a victim of an attack."
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

    private val legacyPreApproval = {
        """
        {
            "id": "f68f6e70-a3d8-4616-b138-358e1799599d",
            "name": "authorize_payment_postApproval",
            "data": "A1*A100.00EUR*ICZ3855000000003643174999",
            "status": "PENDING",
            "operationCreated": "2023-04-25T12:29:23+0000",
            "operationExpires": "2023-04-25T12:34:23+0000",
            "ui": {
                "flipButtons": true,
                "blockApprovalOnCall": false,
                "preApprovalScreen": {
                    "type": "WARNING",
                    "heading": "H",
                    "message": "M",
                    "items": [
                        "A",
                        "B",
                        "C"
                    ],
                    "approvalType": "SLIDER"
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
}
