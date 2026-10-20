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

/**
 * A top-level `mobileTokenData` value whose timestamps were captured with device time
 * and must be aligned to server time before the value is sent to the server.
 *
 * The alignment is performed by the operations service during operation authorization
 * or rejection, after the time has been synchronized with the server.
 */
internal interface ServerTimeAdjustable {

    /**
     * Returns a copy of the value with all timestamps shifted by [localTimeAdjustment].
     *
     * @param localTimeAdjustment Difference between server and device time in milliseconds.
     */
    fun adjusted(localTimeAdjustment: Long): Any
}
