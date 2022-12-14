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

import com.wultra.android.mtokensdk.push.PushMessage
import com.wultra.android.mtokensdk.push.PushMessageOperationCreated
import com.wultra.android.mtokensdk.push.PushMessageOperationFinished
import com.wultra.android.mtokensdk.push.PushParser
import org.junit.Assert
import org.junit.Test


class PushTests {

    @Test
    fun `test empty data`() {
        Assert.assertNull(PushParser.parseNotification(emptyMap()))
    }

    @Test
    fun `test init push valid`() {
        val oid = "1"
        val oname = "test"
        val push = makePush("mtoken.operationInit", oid, oname, null)
        if (push == null) {
            Assert.fail("Failed to parse valid push.")
            return
        }
        if (push !is PushMessageOperationCreated) {
            Assert.fail("Expected operation created push.")
            return
        }

        Assert.assertEquals(oid, push.id)
        Assert.assertEquals(oname, push.name)
    }

    @Test
    fun `test init push missing id`() {
        Assert.assertNull(makePush("mtoken.operationInit", null, "name", null))
    }

    @Test
    fun `test init push missing name`() {
        Assert.assertNull(makePush("mtoken.operationInit", "1",  null, null))
    }

    @Test
    fun `test finish push valid`() {
        val results = mapOf(
            Pair("authentication.success", PushMessageOperationFinished.Result.SUCCESS),
            Pair("authentication.fail", PushMessageOperationFinished.Result.FAIL),
            Pair("operation.timeout", PushMessageOperationFinished.Result.TIMEOUT),
            Pair("operation.canceled", PushMessageOperationFinished.Result.CANCELED),
            Pair("operation.methodNotAvailable", PushMessageOperationFinished.Result.METHOD_NOT_AVAILABLE),
            Pair("nonextistingreason", PushMessageOperationFinished.Result.UNKNOWN)
        )
        for (pair in results) {
            val oid = "1"
            val oname = "test"
            val push = makePush("mtoken.operationFinished", oid, oname, pair.key)
            if (push == null) {
                Assert.fail("Failed to parse valid push.")
                continue
            }
            if (push !is PushMessageOperationFinished) {
                Assert.fail("Expected operation finished push.")
                continue
            }

            Assert.assertEquals(oid, push.id)
            Assert.assertEquals(oname, push.name)
            Assert.assertEquals(pair.value, push.result)
        }
    }

    @Test
    fun `test finish push missing result`() {
        Assert.assertNull(makePush("mtoken.operationFinished","1", "name", null))
    }

    @Test
    fun `test finish push missing id`() {
        Assert.assertNull(makePush("mtoken.operationFinished", null, "name", "authentication.success"))
    }

    @Test
    fun `test finish push missing name`() {
        Assert.assertNull(makePush("mtoken.operationFinished", "1", null, "authentication.success"))
    }

    private fun makePush(type: String?, id: String?, name: String?, opResult: String?): PushMessage? {
        val map = mutableMapOf<String,String>()
        type?.let { map["messageType"] = it }
        id?.let { map["operationId"] = it }
        name?.let { map["operationName"] = it }
        opResult?.let { map["mtokenOperationResult"] = it }
        return PushParser.parseNotification(map)
    }
}