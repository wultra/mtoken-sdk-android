/*
 * Copyright 2023 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.inbox

import com.google.gson.annotations.SerializedName

/**
 * Defines how message's body is formatted.
 */
enum class InboxContentType {
    /**
     * Message's body contains simple plain text.
     */
    @SerializedName("text")
    TEXT,

    /**
     * Message's body contains formatted HTML text.
     */
    @SerializedName("html")
    HTML
}
