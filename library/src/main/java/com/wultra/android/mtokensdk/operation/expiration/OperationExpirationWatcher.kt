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

@file:Suppress("unused")

package com.wultra.android.mtokensdk.operation.expiration

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.wultra.android.mtokensdk.common.Logger
import java.util.*
import kotlin.math.max

/**
 * Expiration Watcher is a utility class that can notify you when an operation expires.
 * In the happy scenario when the operation expires, a notification is sent to the phone.
 * In some cases this might fail (as push messages are not guaranteed to be delivered) and then
 * it comes very handy to be notified internally and act upon it.
 *
 * The default behavior of the expiration is based on the system date and time (by using `now()`).
 * So if the user chooses to change it, it might not work. If you have for example your server time, you can provide
 * it via [currentDateProvider] property.
 *
 * Since expiration checking is implemented in the best-effort way, the primary way
 * of operation expiration verification is reloading the operation from the server.
 *
 * To prevent spamming of the utility by the wrong configuration of the time or desynchronization of the
 * server and the client, minimum report time between 2 reports is 5 seconds.
 */
class OperationExpirationWatcher {

    /**
     * Provider of the current date and time provider. Default implementation
     * current system time.
     */
    var currentDateProvider: CurrentDateProvider = OffsetDateProvider()

    /**
     * Delegate that will be notified about the expiration.
     */
    var listener: OperationExpirationWatcherListener? = null

    private val operationsToWatch = mutableListOf<ExpirableOperation>() // source of "truth" of what is being watched
    private var timer: Timer? = null // timer for scheduling
    private val mutex = Object()

    /**
     * Provides currently watched operations.
     * @return Operations that are watched
     */
    fun getWatchedOperations(): List<ExpirableOperation> {
        return synchronized(mutex) {
            return@synchronized operationsToWatch
        }
    }

    /**
     * Add operation for watching.
     * @param operation Operation to watch
     * @return Operations that are watched
     */
    fun add(operation: ExpirableOperation): List<ExpirableOperation> {
        return add(listOf(operation))
    }

    /**
     * Add operations for watching.
     * @param operations: Operations to watch
     * @return Operations that are watched
     */
    fun add(operations: List<ExpirableOperation>): List<ExpirableOperation> {
        return synchronized(mutex) {
            val currentDate = currentDateProvider.getCurrentDate()
            for (op in operations) {
                // we do not remove expired operations.
                // Operation can expire during the networking communication. Such operation
                // would be lost and never reported as expired.
                if (op.isExpired(currentDate)) {
                    Logger.w("OperationExpirationWatcher: You're adding an expired operation to watch.")
                }
            }

            if (operations.isEmpty()) {
                Logger.w("OperationExpirationWatcher: Cannot watch empty array of operations")
                return@synchronized operationsToWatch
            }

            val opsToWatch = mutableListOf<ExpirableOperation>()
            for (op in operations) {
                // filter already added operations
                if (operationsToWatch.any { it.equals(op) }) {
                    Logger.w("OperationExpirationWatcher: Operation cannot be watched - already there.")
                } else {
                    opsToWatch.add(op)
                }
            }

            if (opsToWatch.isEmpty()) {
                Logger.w("OperationExpirationWatcher: All operations are already watched")
            } else {
                Logger.d("OperationExpirationWatcher: Adding ${opsToWatch.count()} operation to watch.")
                operationsToWatch.addAll(opsToWatch)
                prepareTimer()
            }

            return@synchronized operationsToWatch
        }
    }

    /**
     * Stop watching operations for expiration.
     * @param operations operations to watch
     * @return Operations that are watched
     */
    fun remove(operations: List<ExpirableOperation>): List<ExpirableOperation> {
        return stop(operations)
    }

    /**
     * Stop watching an operation for expiration.
     * @param operation: operation to watch
     * @return Operations that are watched
     */
    fun remove(operation: ExpirableOperation): List<ExpirableOperation> {
        return stop(listOf(operation))
    }

    /**
     * Stop watching all operation
     * @return Operations that are watched
     */
    fun removeAll(): List<ExpirableOperation> {
        return stop(null)
    }

    // Private methods

    private fun stop(operations: List<ExpirableOperation>?): List<ExpirableOperation> {
        return synchronized(mutex) {
            // is there anything to stop?
            if (operationsToWatch.isNotEmpty()) {
                // when nil is provided, we consider it as "stop all"
                if (operations != null) {
                    operationsToWatch.removeAll { current -> operations.any { toRemove -> toRemove.equals(current) } }
                    Logger.d("OperationExpirationWatcher: Stopped watching ${operations.count()} operations.")
                } else {
                    operationsToWatch.clear()
                    Logger.d("OperationExpirationWatcher: Stopped watching all operations.")
                }
                prepareTimer()
            }

            return@synchronized operationsToWatch
        }
    }

    private fun prepareTimer() {

        // stop the previous timer
        timer?.cancel()
        timer = null

        if (operationsToWatch.isEmpty()) {
            Logger.d("OperationExpirationWatcher: No operations to watch.")
            return
        }

        val firstOp = operationsToWatch.minByOrNull { it.expires }.let {
            it ?: return@prepareTimer
        }

        Handler(Looper.getMainLooper()).post {

            // This is a precaution when you'll receive an expired operation from the backend over and over again
            // and it would lead to infinite refresh time. This also helps when device and backend time is out of sync heavily.
            // This leads to a minimal "expire report time" of 5 seconds.
            val interval = max(5, firstOp.expires.toEpochSecond() - currentDateProvider.getCurrentDate().toEpochSecond())

            Logger.d("OperationExpirationWatcher: Scheduling operation expire check in ${interval.toInt()} seconds.")

            val t = Timer("OperationsExpirationWatcherTimer", false)
            t.schedule(object : TimerTask() {
                override fun run() {
                    synchronized(mutex) {
                        val currentDate = currentDateProvider.getCurrentDate()
                        val expiredOps = operationsToWatch.filter { it.isExpired(currentDate) }

                        if (expiredOps.isEmpty()) {
                            return@synchronized
                        }

                        operationsToWatch.removeAll { it.isExpired(currentDate) }
                        prepareTimer()
                        Handler(Looper.getMainLooper()).post {
                            Logger.d("OperationExpirationWatcher: Reporting ${expiredOps.count()} expired operations.")
                            listener?.operationsExpired(expiredOps)
                        }
                    }
                }
            }, interval * 1000)
            timer = t
        }
    }
}

/**
 * Interface for listener which gets called when operation expires
 */
interface OperationExpirationWatcherListener {
    /**
     * Called when operation(s) expire(s).
     * The method is called on the main thread by the `OperationExpirationWatcher`.
     * @param expiredOperations array of operations that expired
     */
    @MainThread
    fun operationsExpired(expiredOperations: List<ExpirableOperation>)
}