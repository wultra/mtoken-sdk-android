/*
 * Copyright 2026 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.api.operation.model.mobiletokendata

import com.google.gson.JsonParser
import com.wultra.android.mtokensdk.log.WMTLogger
import com.wultra.android.mtokensdk.operation.OperationsUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class PreApprovalScreensRecorderTests {

    private var originalVerboseLevel = WMTLogger.verboseLevel

    @Before
    fun setup() {
        // android.util.Log is not available in JVM unit tests
        originalVerboseLevel = WMTLogger.verboseLevel
        WMTLogger.verboseLevel = WMTLogger.VerboseLevel.OFF
    }

    @After
    fun tearDown() {
        WMTLogger.verboseLevel = originalVerboseLevel
    }

    private fun Any.field(name: String): Any? = javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)
    private fun Any.opened() = field("timestampOpened") as ZonedDateTime
    private fun Any.closed() = field("timestampClosed") as ZonedDateTime?

    private fun recordFlow() = PreApprovalScreensRecorder()
        .begin("intro")
        .end("intro", PreApprovalScreensRecorder.Action.CONTINUE)
        .begin("qr")
        // "qr" stays unclosed because a new visit begins
        .begin("confirm")
        .end("confirm", PreApprovalScreensRecorder.Action.REJECT)

    @Test
    fun testTimestampsUseDeviceTime() {
        val before = Instant.now()
        val visits = recordFlow().build() as List<*>
        val after = Instant.now()

        Assert.assertEquals(3, visits.size)
        visits.forEach { visit ->
            val timestamps = listOfNotNull(visit!!.opened(), visit.closed()).map { it.toInstant() }
            timestamps.forEach {
                Assert.assertFalse("Timestamp $it should not be before $before", it.isBefore(before))
                Assert.assertFalse("Timestamp $it should not be after $after", it.isAfter(after))
            }
        }
    }

    @Test
    fun testAdjustedShiftsTimestampsByOffset() {
        val offsetMs = 5 * 60 * 1000L
        val raw = recordFlow().build()
        val adjusted = (raw as ServerTimeAdjustable).adjusted(offsetMs)

        Assert.assertTrue("Adjusted value should be a list", adjusted is List<*>)
        Assert.assertFalse("Adjusted value should not be adjustable again", adjusted is ServerTimeAdjustable)

        val rawVisits = raw as List<*>
        val adjustedVisits = adjusted as List<*>
        Assert.assertEquals(rawVisits.size, adjustedVisits.size)

        rawVisits.zip(adjustedVisits).forEach { (r, a) ->
            Assert.assertEquals(r!!.field("screen"), a!!.field("screen"))
            Assert.assertEquals(r.field("action"), a.field("action"))
            Assert.assertEquals(offsetMs, Duration.between(r.opened(), a.opened()).toMillis())
            val rawClosed = r.closed()
            if (rawClosed == null) {
                Assert.assertNull("Unclosed visit should stay unclosed", a.closed())
            } else {
                Assert.assertEquals(offsetMs, Duration.between(rawClosed, a.closed()!!).toMillis())
            }
        }

        // Unclosed "qr" visit
        Assert.assertEquals("qr", adjustedVisits[1]!!.field("screen"))
        Assert.assertNull(adjustedVisits[1]!!.closed())
        Assert.assertNull(adjustedVisits[1]!!.field("action"))
    }

    @Test
    fun testRepeatedBuildsAreConsistent() {
        val recorder = recordFlow()
        val first = recorder.build() as List<*>
        val second = recorder.build() as List<*>

        Assert.assertEquals(first, second)
        Assert.assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun testBuildClosesOpenVisitWithoutAction() {
        val visits = PreApprovalScreensRecorder()
            .begin("intro")
            .build() as List<*>

        Assert.assertEquals(1, visits.size)
        Assert.assertNotNull(visits[0]!!.closed())
        Assert.assertNull(visits[0]!!.field("action"))
    }

    @Test
    fun testRawValueSerializesAsJsonArrayWithUtcInstants() {
        val built = recordFlow().build()
        val gson = OperationsUtils.defaultGsonBuilder().create()
        val json = gson.toJson(mapOf("preApprovalScreens" to built))

        val array = JsonParser.parseString(json).asJsonObject.getAsJsonArray("preApprovalScreens")
        Assert.assertEquals(3, array.size())
        val first = array[0].asJsonObject
        Assert.assertEquals("intro", first["screen"].asString)
        Assert.assertEquals("CONTINUE", first["action"].asString)

        // Epoch comparison distinguishes the UTC instant serializer from the legacy local-time format
        val visit = (built as List<*>)[0]!!
        Assert.assertEquals(visit.opened().toInstant().toEpochMilli(), Instant.parse(first["timestampOpened"].asString).toEpochMilli())
        Assert.assertEquals(visit.closed()!!.toInstant().toEpochMilli(), Instant.parse(first["timestampClosed"].asString).toEpochMilli())
    }
}
