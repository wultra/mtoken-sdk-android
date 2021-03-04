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