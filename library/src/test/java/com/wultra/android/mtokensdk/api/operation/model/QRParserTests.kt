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

package com.wultra.android.mtokensdk.api.operation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.lang.Exception

class QRParserTests {

    @Test
    fun `test current format`() {
        val code = makeCode()
        try {
            val op = QROperationParser.parse(code)
            assertEquals("5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6", op.operationId)
        } catch (e: Exception) {
            fail("This should be parsed. $e")
        }
    }

    fun makeCode(
            operationId: String          = "5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6",
            title: String                = "Payment",
            message: String              = "Please confirm this payment",
            operationData: String        = "A1*A100CZK*ICZ2730300000001165254011*D20180425*Thello world",
            flags: String                = "B",
            otherAttrs: List<String>?   = null,
            nonce: String                = "AD8bOO0Df73kNaIGb3Vmpg==",
            signingKey: String           = "0",
            signature: String            = "MEYCIQDby1Uq+MaxiAAGzKmE/McHzNOUrvAP2qqGBvSgcdtyjgIhAMo1sgqNa1pPZTFBhhKvCKFLGDuHuTTYexdmHFjUUIJW"
    ): String {
        val attrs = otherAttrs?.joinToString("\n", postfix = "\n") ?: ""
        return "${operationId}\n${title}\n${message}\n${operationData}\n${flags}\n${attrs}${nonce}\n${signingKey}${signature}"
    }
}