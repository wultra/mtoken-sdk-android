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

import com.wultra.android.mtokensdk.api.operation.model.QROperationData
import com.wultra.android.mtokensdk.api.operation.model.QROperationParser
import com.wultra.android.mtokensdk.api.operation.model.QROperationSignature
import org.junit.Assert.*
import org.junit.Test
import java.lang.Exception
import java.math.BigDecimal
import java.util.*

class QRParserTests {

    /*
     * Main tests
     */

    @Test
    fun `test current format`() {

        val code = makeCode()

        /* ktlint-disable indent */

        val expectedSignedData = (
            "5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6\n" +
            "Payment\n" +
            "Please confirm this payment\n" +
            "A1*A100CZK*ICZ2730300000001165254011*D20180425*Thello world\n" +
            "BCFX\n" +
            "AD8bOO0Df73kNaIGb3Vmpg==\n" +
            "0"
            ).toByteArray()

        /* ktlint-enable */

        try {
            val operation = QROperationParser.parse(code)
            assertEquals("5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6", operation.operationId)
            assertEquals("5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6", operation.operationId)
            assertEquals("Payment", operation.title)
            assertEquals("Please confirm this payment", operation.message)
            assert(operation.flags.biometryAllowed) { "biometry allowed flag missing" }
            assert(operation.flags.blockWhenOnCall) { "block when on call flag missing" }
            assert(operation.flags.flipButtons) { "flip buttons flag missing" }
            assert(operation.flags.fraudWarning) { "fraud warning flag missing" }
            assertEquals("AD8bOO0Df73kNaIGb3Vmpg==", operation.nonce)
            assertEquals("MEYCIQDby1Uq+MaxiAAGzKmE/McHzNOUrvAP2qqGBvSgcdtyjgIhAMo1sgqNa1pPZTFBhhKvCKFLGDuHuTTYexdmHFjUUIJW", operation.signature.signatureString)
            assertEquals(QROperationSignature.SigningKey.MASTER, operation.signature.signingKey)
            assert(operation.signedData.contentEquals(expectedSignedData))

            // Operation data
            assertEquals(QROperationData.Version.V1, operation.operationData.version)
            assertEquals(1, operation.operationData.templateId)
            assertEquals(4, operation.operationData.fields.count())
            assertEquals("A1*A100CZK*ICZ2730300000001165254011*D20180425*Thello world", operation.operationData.sourceString)

            val fields = operation.operationData.fields
            fields[0].let {
                if (it is QROperationData.AmountField) {
                    assertEquals(BigDecimal(100), it.amount)
                    assertEquals("CZK", it.currency)
                } else {
                    fail("Amount was not parsed correctly")
                }
            }
            fields[1].let {
                if (it is QROperationData.AccountField) {
                    assertEquals("CZ2730300000001165254011", it.iban)
                    assertEquals(null, it.bic)
                } else {
                    fail("Account was not parsed correctly")
                }
            }
            fields[2].let {
                if (it is QROperationData.DateField) {
                    assertEquals(Date(118, 3, 25), it.date)
                } else {
                    fail("Date was not parsed correctly")
                }
            }
            fields[3].let {
                if (it is QROperationData.TextField) {
                    assertEquals("hello world", it.text)
                } else {
                    fail("Text was not parsed correctly")
                }
            }
        } catch (e: Exception) {
            fail("This should be parsed. $e")
        }
    }

    @Test
    fun `test forward compatibility`() {
        val qrcode = makeCode(operationData = "B2*Xtest", otherAttrs = listOf("12345678", "Some Additional Information"), flags = "B")
        /* ktlint-disable indent */
        val expectedSignedData = (
            "5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6\n" +
            "Payment\n" +
            "Please confirm this payment\n" +
            "B2*Xtest\n" +
            "B\n" +
            "12345678\n" +
            "Some Additional Information\n" +
            "AD8bOO0Df73kNaIGb3Vmpg==\n" +
            "0"
        ).toByteArray()
        /* ktlint-enable */

        try {
            val operation = QROperationParser.parse(qrcode)

            assert(operation.isNewerFormat)
            assert(operation.signedData.contentEquals(expectedSignedData))
            assertEquals(QROperationData.Version.VX, operation.operationData.version)
            assertEquals(1, operation.operationData.fields.count())
            operation.operationData.fields[0].let {
                if (it is QROperationData.FallbackField) {
                    assertEquals("test", it.text)
                    assertEquals('X', it.type)
                } else {
                    fail("OperationData parser is not forward compatible")
                }
            }
        } catch (e: Exception) {
            fail("This should be parsed. $e")
        }
    }

    /**
     * Missing or Bad attributes
     */

    @Test
    fun `test missing operation id`() {
        try {
            QROperationParser.parse(makeCode(operationId = ""))
            fail("Exception expected")
        } catch (e: Exception) {
            // expected
        }
    }

    @Test
    fun `test missing title or message`() {
        try {
            val operation = QROperationParser.parse(makeCode(title = "", message = ""))
            assertEquals("", operation.title)
            assertEquals("", operation.message)
        } catch (e: Exception) {
            fail("This should be parsed")
        }
    }

    @Test
    fun `test missing or bad operation data version`() {
        listOf("", "A", "2", "A100", "A-100").forEach {
            try {
                QROperationParser.parse(makeCode(operationData = it))
                fail("Operation data $it should not be accepted")
            } catch (e: Exception) {
                // expected
            }
        }
    }

    @Test
    fun `test missing flags`() {
        try {
            val operation = QROperationParser.parse(makeCode(flags = ""))
            assertFalse(operation.flags.biometryAllowed)
            assertFalse(operation.flags.blockWhenOnCall)
            assertFalse(operation.flags.flipButtons)
            assertFalse(operation.flags.fraudWarning)
        } catch (e: Exception) {
            fail("This should be parsed")
        }
    }

    @Test
    fun `test missing or bad nonce`() {
        listOf("", "AAAA", "MEYCIQDby1Uq+MaxiAAGzKmE/McHzNOUrvAP2qqGBvSgcdtyjgIhAMo1sgqNa1pPZTFBhhKvCKFLGDuHuTTYexdmHFjUUIJW").forEach {
            try {
                QROperationParser.parse(makeCode(nonce = it))
                fail("Nonce $it should not be accepted")
            } catch (e: Exception) {
                // expected
            }
        }
    }

    @Test
    fun `test missing or bad signature`() {
        try {
            QROperationParser.parse(makeCode(signingKey = "", signature = ""))
            fail("This should not be parsed")
        } catch (e: Exception) {
            // expected
        }

        listOf("", "AAAA", "AD8bOO0Df73kNaIGb3Vmpg==").forEach {
            try {
                QROperationParser.parse(makeCode(signature = it))
                fail("Signature $it should not be accepted")
            } catch (e: Exception) {
                // expected
            }
        }

        listOf("", "2", "X").forEach {
            try {
                QROperationParser.parse(makeCode(signingKey = it))
                fail("Signing key $it should not be accepted")
            } catch (e: Exception) {
                // expected
            }
        }
    }

    /**
     * String escaping
     */

    @Test
    fun `test attribute string escaping`() {
        try {
            val operation = QROperationParser.parse(makeCode(title = "Hello\\nWorld\\\\xyz", message = "Hello\\nWorld\\\\xyz\\*"))
            assertEquals("Hello\nWorld\\xyz", operation.title)
            assertEquals("Hello\nWorld\\xyz\\*", operation.message)
        } catch (e: Exception) {
            fail("This should be parsed")
        }
    }

    @Test
    fun `test field string escaping`() {
        val code = makeCode(operationData = "A1*Thello \\* asterisk*Nnew\\nline*Xback\\\\slash")

        try {
            val operation = QROperationParser.parse(code)

            assertEquals(3, operation.operationData.fields.count())

            val fields = operation.operationData.fields
            fields[0].let {
                if (it is QROperationData.TextField) {
                    assertEquals("hello * asterisk", it.text)
                } else {
                    fail()
                }
            }
            fields[1].let {
                if (it is QROperationData.NoteField) {
                    assertEquals("new\nline", it.text)
                } else {
                    fail()
                }
            }
            fields[2].let {
                if (it is QROperationData.FallbackField) {
                    assertEquals("back\\slash", it.text)
                } else {
                    fail()
                }
            }
        } catch (e: Exception) {
            fail("This should be parsed. $e")
        }
    }

    /**
     * Field types
     */

    @Test
    fun `test field amount`() {
        val valid: List<Triple<String, BigDecimal, String>> = listOf(
            Triple("A100CZK", BigDecimal("100"), "CZK"),
            Triple("A100.00EUR", BigDecimal("100.00"), "EUR"),
            Triple("A99.32USD", BigDecimal("99.32"), "USD"),
            Triple("A-50000.16GBP", BigDecimal("-50000.16"), "GBP"),
            Triple("A.325CZK", BigDecimal("0.325"), "CZK")
        )
        valid.forEach {
            try {
                val operation = QROperationParser.parse(makeCode(operationData = "A1*${it.first}"))
                operation.operationData.fields[0].let { field ->
                    if (field is QROperationData.AmountField) {
                        assertEquals(it.second, field.amount)
                        assertEquals(it.third, field.currency)
                    } else {
                        fail("Unexpected operation data")
                    }
                }
            } catch (e: Exception) {
                fail("Amount ${it.first} should be parsed")
            }
        }
        // Invalid
        listOf("ACZK", "A", "A0", "AxCZK").forEach { field ->
            try {
                QROperationParser.parse(makeCode(operationData = "A1*$field"))
                fail("This should not be parsed")
            } catch (e: Exception) {
                // expected
            }
        }
    }

    @Test
    fun `test field account`() {
        val valid: List<Triple<String, String, String?>> = listOf(
            Triple("ISOMEIBAN1234,BIC", "SOMEIBAN1234", "BIC"),
            Triple("ISOMEIBAN", "SOMEIBAN", null),
            Triple("ISOMEIBAN,", "SOMEIBAN", null)
        )
        valid.forEach {
            try {
                val operation = QROperationParser.parse(makeCode(operationData = "A1*${it.first}"))
                operation.operationData.fields[0].let { field ->
                    if (field is QROperationData.AccountField) {
                        assertEquals(it.second, field.iban)
                        assertEquals(it.third, field.bic)
                    } else {
                        fail("Unexpected operation data")
                    }
                }
            } catch (e: Exception) {
                fail("Account ${it.first} should be parsed")
            }
        }
        // Invalid
        listOf("I", "Isomeiban,", "IGOODIBAN,badbic").forEach { field ->
            try {
                QROperationParser.parse(makeCode(operationData = "A1*$field"))
                fail("This should not be parsed")
            } catch (e: Exception) {
                // expected
            }
        }
    }

    @Test
    fun `test field date`() {
        // Invalid dates
        listOf("D", "D0", "D2004", "D20189999").forEach {
            try {
                QROperationParser.parse(makeCode(operationData = "A1*$it"))
                fail("Date $it should not be accepted")
            } catch (e: Exception) {
                // expected
            }
        }
    }

    @Test
    fun `test field empty`() {
        try {
            val operation = QROperationParser.parse(makeCode(operationData = "A1*A10CZK****Ttest"))
            val fields = operation.operationData.fields
            assertEquals(5, fields.count())
            assert(fields[0] is QROperationData.AmountField)
            assert(fields[1] is QROperationData.EmptyField)
            assert(fields[2] is QROperationData.EmptyField)
            assert(fields[3] is QROperationData.EmptyField)
            assert(fields[4] is QROperationData.TextField)
        } catch (e: Exception) {
            fail("This should be parsed")
        }
    }

    /* ktlint-disable indent no-multi-spaces */

    /**
     * Helper methods
     */
    private fun makeCode(
            operationId: String          = "5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6",
            title: String                = "Payment",
            message: String              = "Please confirm this payment",
            operationData: String        = "A1*A100CZK*ICZ2730300000001165254011*D20180425*Thello world",
            flags: String                = "BCFX",
            otherAttrs: List<String>?   = null,
            nonce: String                = "AD8bOO0Df73kNaIGb3Vmpg==",
            signingKey: String           = "0",
            signature: String            = "MEYCIQDby1Uq+MaxiAAGzKmE/McHzNOUrvAP2qqGBvSgcdtyjgIhAMo1sgqNa1pPZTFBhhKvCKFLGDuHuTTYexdmHFjUUIJW"
    ): String {
        val attrs = otherAttrs?.joinToString("\n", postfix = "\n") ?: ""
        return "${operationId}\n${title}\n${message}\n${operationData}\n${flags}\n${attrs}${nonce}\n${signingKey}${signature}"
    }

    /* ktlint-enable */
}
