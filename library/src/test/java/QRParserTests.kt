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
import com.wultra.android.mtokensdk.api.operation.model.QROperationParseException
import com.wultra.android.mtokensdk.api.operation.model.QROperationParser
import com.wultra.android.mtokensdk.api.operation.model.QROperationSignature
import com.wultra.android.mtokensdk.api.operation.model.QRParseError
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class QRParserTests {

    /** Inline reified helper to replace kotlin.test assertIsInstance<T> (not available with JUnit 4 only). */
    private inline fun <reified T> assertIsInstance(value: Any?, message: String? = null): T {
        assertTrue(message ?: "Expected ${T::class.simpleName} but got ${value?.let { it::class.simpleName }}", value is T)
        return value as T
    }

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

        val operation = QROperationParser.parse(code)
        assertEquals("5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6", operation.operationId)
        assertEquals("Payment", operation.title)
        assertEquals("Please confirm this payment", operation.message)
        assertTrue(operation.flags.biometricsAllowed)
        assertTrue(operation.flags.blockWhenOnCall)
        assertTrue(operation.flags.flipButtons)
        assertTrue(operation.flags.fraudWarning)
        assertEquals("AD8bOO0Df73kNaIGb3Vmpg==", operation.nonce)
        assertEquals("MEYCIQDby1Uq+MaxiAAGzKmE/McHzNOUrvAP2qqGBvSgcdtyjgIhAMo1sgqNa1pPZTFBhhKvCKFLGDuHuTTYexdmHFjUUIJW", operation.signature.dataSource)
        assertEquals(QROperationSignature.KeyType.MASTER, operation.signature.keyType)
        assertTrue(operation.signedData.contentEquals(expectedSignedData))

        // Operation data
        assertEquals(QROperationData.Version.V1, operation.operationData.version)
        assertEquals(1, operation.operationData.templateId)
        assertEquals(4, operation.operationData.fields.count())
        assertEquals("A1*A100CZK*ICZ2730300000001165254011*D20180425*Thello world", operation.operationData.sourceString)

        val fields = operation.operationData.fields
        val amount = assertIsInstance<QROperationData.AmountField>(fields[0])
        assertEquals(BigDecimal(100), amount.amount)
        assertEquals("CZK", amount.currency)

        val account = assertIsInstance<QROperationData.AccountField>(fields[1])
        assertEquals("CZ2730300000001165254011", account.iban)
        assertNull(account.bic)

        val date = assertIsInstance<QROperationData.DateField>(fields[2])
        assertEquals(Date(118, 3, 25), date.date)

        val text = assertIsInstance<QROperationData.TextField>(fields[3])
        assertEquals("hello world", text.text)
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

        val operation = QROperationParser.parse(qrcode)

        assertTrue(operation.isNewerFormat)
        assertTrue(operation.signedData.contentEquals(expectedSignedData))
        assertEquals(QROperationData.Version.VX, operation.operationData.version)
        assertEquals(1, operation.operationData.fields.count())

        val fallback = assertIsInstance<QROperationData.FallbackField>(operation.operationData.fields[0])
        assertEquals("test", fallback.text)
        assertEquals('X', fallback.type)
    }

    /**
     * Missing or Bad attributes
     */

    @Test
    fun `test missing operation id`() {
        val e = assertThrows(QROperationParseException::class.java) {
            QROperationParser.parse(makeCode(operationId = ""))
        }
        assertEquals(QRParseError.EMPTY_OPERATION_ID, e.reason)
    }

    @Test
    fun `test missing title or message`() {
        val operation = QROperationParser.parse(makeCode(title = "", message = ""))
        assertEquals("", operation.title)
        assertEquals("", operation.message)
    }

    @Test
    fun `test missing or bad operation data version`() {
        listOf("", "A", "2", "A100", "A-100").forEach {
            val e = assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(operationData = it))
            }
            assertEquals("Operation data '$it' should fail with INVALID_OPERATION_DATA", QRParseError.INVALID_OPERATION_DATA, e.reason)
        }
    }

    @Test
    fun `test missing flags`() {
        val operation = QROperationParser.parse(makeCode(flags = ""))
        assertFalse(operation.flags.biometricsAllowed)
        assertFalse(operation.flags.blockWhenOnCall)
        assertFalse(operation.flags.flipButtons)
        assertFalse(operation.flags.fraudWarning)
    }

    @Test
    fun `test missing or bad nonce`() {
        listOf("", "AAAA", "MEYCIQDby1Uq+MaxiAAGzKmE/McHzNOUrvAP2qqGBvSgcdtyjgIhAMo1sgqNa1pPZTFBhhKvCKFLGDuHuTTYexdmHFjUUIJW").forEach {
            val e = assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(nonce = it))
            }
            assertEquals("Nonce '$it' should fail with INVALID_NONCE", QRParseError.INVALID_NONCE, e.reason)
        }
    }

    @Test
    fun `test missing or bad signature`() {
        val emptyE = assertThrows(QROperationParseException::class.java) {
            QROperationParser.parse(makeCode(signingKey = "", signature = ""))
        }
        assertEquals(QRParseError.INVALID_SIGNATURE, emptyE.reason)

        listOf("", "AAAA", "AD8bOO0Df73kNaIGb3Vmpg==").forEach {
            val e = assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(signature = it))
            }
            assertEquals("Signature '$it' should fail with INVALID_SIGNATURE", QRParseError.INVALID_SIGNATURE, e.reason)
        }

        listOf("", "2", "X").forEach {
            val e = assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(signingKey = it))
            }
            assertEquals("Signing key '$it' should fail with INVALID_SIGNATURE", QRParseError.INVALID_SIGNATURE, e.reason)
        }
    }

    /**
     * String escaping
     */

    @Test
    fun `test attribute string escaping`() {
        val operation = QROperationParser.parse(makeCode(title = "Hello\\nWorld\\\\xyz", message = "Hello\\nWorld\\\\xyz\\*"))
        assertEquals("Hello\nWorld\\xyz", operation.title)
        assertEquals("Hello\nWorld\\xyz\\*", operation.message)
    }

    @Test
    fun `test field string escaping`() {
        val code = makeCode(operationData = "A1*Thello \\* asterisk*Nnew\\nline*Xback\\\\slash")

        val operation = QROperationParser.parse(code)

        assertEquals(3, operation.operationData.fields.count())

        val fields = operation.operationData.fields
        val textField = assertIsInstance<QROperationData.TextField>(fields[0])
        assertEquals("hello * asterisk", textField.text)

        val noteField = assertIsInstance<QROperationData.NoteField>(fields[1])
        assertEquals("new\nline", noteField.text)

        val fallbackField = assertIsInstance<QROperationData.FallbackField>(fields[2])
        assertEquals("back\\slash", fallbackField.text)
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
            val operation = QROperationParser.parse(makeCode(operationData = "A1*${it.first}"))
            val field = assertIsInstance<QROperationData.AmountField>(operation.operationData.fields[0])
            assertEquals(it.second, field.amount)
            assertEquals(it.third, field.currency)
        }
        // Invalid
        listOf("ACZK", "A", "A0", "AxCZK").forEach { field ->
            assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(operationData = "A1*$field"))
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
            val operation = QROperationParser.parse(makeCode(operationData = "A1*${it.first}"))
            val field = assertIsInstance<QROperationData.AccountField>(operation.operationData.fields[0])
            assertEquals(it.second, field.iban)
            assertEquals(it.third, field.bic)
        }
        // Invalid
        listOf("I", "Isomeiban,", "IGOODIBAN,badbic").forEach { field ->
            assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(operationData = "A1*$field"))
            }
        }
    }

    @Test
    fun `test field date`() {
        // Invalid dates
        listOf("D", "D0", "D2004", "D20189999").forEach {
            assertThrows(QROperationParseException::class.java) {
                QROperationParser.parse(makeCode(operationData = "A1*$it"))
            }
        }
    }

    @Test
    fun `test field empty`() {
        val operation = QROperationParser.parse(makeCode(operationData = "A1*A10CZK****Ttest"))
        val fields = operation.operationData.fields
        assertEquals(5, fields.count())
        assertIsInstance<QROperationData.AmountField>(fields[0])
        assertIsInstance<QROperationData.EmptyField>(fields[1])
        assertIsInstance<QROperationData.EmptyField>(fields[2])
        assertIsInstance<QROperationData.EmptyField>(fields[3])
        assertIsInstance<QROperationData.TextField>(fields[4])
    }

    @Test
    fun `test invalid format too few fields`() {
        val e = assertThrows(QROperationParseException::class.java) {
            QROperationParser.parse("only\nthree\nfields")
        }
        assertEquals(QRParseError.INVALID_FORMAT, e.reason)
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
