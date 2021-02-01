/*
 * Copyright (c) 2021, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.test

import com.wultra.android.mtokensdk.operation.expiration.ExpirableOperation
import com.wultra.android.mtokensdk.operation.expiration.OperationExpirationWatcher
import com.wultra.android.mtokensdk.operation.expiration.OperationExpirationWatcherListener
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.zone.TzdbZoneRulesProvider
import org.threeten.bp.zone.ZoneRulesProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class OperationExpirationTests {

    private val watcher = OperationExpirationWatcher()

    init {
        initThreeTen()
    }

    @After
    fun clear() {
        watcher.listener = null
        watcher.removeAll()
    }

    @Test
    fun testAddOperation() {
        val op = Operation()
        watcher.add(op)
        val ops = watcher.getWatchedOperations()
        Assert.assertTrue(ops.count() == 1 && ops.first().equals(op))
    }

    @Test
    fun testAddSameOperationTwice() {
        val op = Operation()
        watcher.add(op)
        val ops = watcher.add(op)
        Assert.assertTrue(ops.count() == 1 && ops.first().equals(op))
    }

    @Test
    fun testAddOperations() {
        val ops = watcher.add(listOf(Operation(), Operation()))
        Assert.assertTrue(ops.count() == 2)
    }

    @Test
    fun testRemoveOperation() {
        val op = Operation()
        val ops = watcher.add(op)
        Assert.assertTrue(ops.count() == 1 && ops.first().equals(op))
        val opsAfterRemoved = watcher.remove(op)
        Assert.assertTrue(opsAfterRemoved.isEmpty())
    }

    @Test
    fun testRemoveNonAddedOperation() {
        val op = Operation()
        val ops = watcher.add(op)
        Assert.assertTrue(ops.count() == 1 && ops.first().equals(op))
        val opsAfterRemoved = watcher.remove(Operation())
        Assert.assertTrue(opsAfterRemoved.count() == 1)
    }

    @Test
    fun testRemoveOperations() {
        val op = Operation()
        val op2 = Operation()
        watcher.add(op)
        val ops = watcher.add(op2)
        Assert.assertTrue(ops.count() == 2)
        val opsAfterRemoved = watcher.remove(listOf(op, op2))
        Assert.assertTrue(opsAfterRemoved.isEmpty())
    }

    @Test
    fun testRemoveAllOperations() {
        watcher.add(Operation())
        val ops = watcher.add(listOf(Operation(), Operation()))
        Assert.assertTrue(ops.count() == 3)
        val opsAfterRemoved = watcher.removeAll()
        Assert.assertTrue(opsAfterRemoved.isEmpty())
    }

    @Test
    fun testExpiring() {
        val future = CompletableFuture<Any?>()
        val op = Operation()
        watcher.listener = WatcherListener { ops ->
                if (ops.count() != 1 || !ops.first().equals(op)) {
                    future.completeExceptionally(Throwable())
                    return@WatcherListener
                }
                val curOps = watcher.getWatchedOperations()
                if (curOps.isNotEmpty()) {
                    future.completeExceptionally(Throwable())
                    return@WatcherListener
                }
                future.complete(null)
        }
        watcher.add(op)
        // we need to wait longer, because minimum report time is 5 seconds
        Assert.assertNull(future.get(10, TimeUnit.SECONDS))
    }

    @Test
    fun testExpiring2() {
        val future = CompletableFuture<Any?>()
        watcher.listener = WatcherListener { ops ->
            if (ops.count() != 1) {
                future.completeExceptionally(Throwable())
                return@WatcherListener
            }
            val curOps = watcher.getWatchedOperations()
            if (curOps.count() != 1) {
                future.completeExceptionally(Throwable())
                return@WatcherListener
            }
            future.complete(null)
        }
        watcher.add(listOf(Operation(), Operation(ZonedDateTime.now().plusSeconds(20))))
        // we need to wait longer, because minimum report time is 5 seconds
        Assert.assertNull(future.get(10, TimeUnit.SECONDS))
    }
}

private class WatcherListener(
        private val callback: (List<ExpirableOperation>) -> Unit): OperationExpirationWatcherListener {

    override fun operationsExpired(expiredOperations: List<ExpirableOperation>) {
        callback(expiredOperations)
    }
}

private class Operation(
        override val expires: ZonedDateTime = ZonedDateTime.now()
): ExpirableOperation

fun Any.initThreeTen() {
    if (ZoneRulesProvider.getAvailableZoneIds().isEmpty()) {
        val stream = this.javaClass.classLoader!!.getResourceAsStream("TZDB.dat")
        stream.use(::TzdbZoneRulesProvider).apply {
            ZoneRulesProvider.registerProvider(this)
        }
    }
}