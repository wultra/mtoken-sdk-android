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

package com.wultra.android.mtokensdk.operation.expiration

import java.time.ZonedDateTime

/**
 * Provides current time as milliseconds since epoch.
 *
 * Implementations can return system time, server-synchronized time, or any custom source.
 *
 * If you need a ZonedDateTime, you can convert it manually:
 * `ZonedDateTime.ofInstant(Instant.ofEpochMilli(getCurrentDate()), ZoneId.systemDefault())`
 */
interface CurrentDateProvider {
    fun getCurrentDate(): Long
}

/**
 * Default implementation of a date provider.
 * You can customize this provider by the `Long` offset (in seconds) that is added to
 * the new `.now()` instance that is returned for `getCurrentDate`.
 */
class OffsetDateProvider(private val offset: Long = 0): CurrentDateProvider {
    override fun getCurrentDate(): Long = ZonedDateTime.now().plusSeconds(offset).toInstant().toEpochMilli()
}
