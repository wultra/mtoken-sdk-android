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

import org.threeten.bp.ZonedDateTime

/**
 * Provides current date. Can be a system date, server date or whatever
 * you choose. Default implementation of this protocol returns system date.
 */
interface CurrentDateProvider {
    fun getCurrentDate(): ZonedDateTime
}

/**
 * Default implementation of a date provider.
 * You can customize this provider by the `Long` offset (in seconds) that is added to
 * the new `.now()` instance that is returned for `getCurrentDate`.
 */
class OffsetDateProvider(private val offset: Long = 0): CurrentDateProvider {
    override fun getCurrentDate(): ZonedDateTime = ZonedDateTime.now().plusSeconds(offset)
}