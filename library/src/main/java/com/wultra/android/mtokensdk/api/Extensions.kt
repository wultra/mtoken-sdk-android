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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wultra.android.mtokensdk.api

import com.wultra.android.powerauth.networking.error.ApiError
import com.wultra.android.powerauth.networking.error.ApiErrorException

/**
 * Create `ApiError` instance from Throwable object.
 */
internal fun Throwable.apiErrorForListener(): ApiError {
    return if (this is ApiErrorException && cause != null) {
        // If ApiErrorException is reported, then we can expect that it only wraps an original
        // exception. For a compatibility with old listener API, we have to simply create
        // ApiError from that wrapped exception.
        ApiError(cause!!)
    } else {
        // Otherwise simply create ApiError from this object.
        ApiError(this)
    }
}