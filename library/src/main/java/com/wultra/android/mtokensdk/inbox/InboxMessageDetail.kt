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

package com.wultra.android.mtokensdk.inbox

import java.util.Date

/**
 * Class containing message detail.
 */
data class InboxMessageDetail(
    /**
     * Message's identifier.
     */
    val id: String,
    /**
     * Message's subject.
     */
    val subject: String,
    /**
     * Message's summary. It typically contains a reduced information from message's body,
     * with no additional formatting.
     */
    val summary: String,
    /**
     * Message's body.
     */
    val body: String,
    /**
     * Message body's content type.
     */
    val type: InboxContentType,
    /**
     * If `true`, then user already read the message.
     */
    val read: Boolean,
    /**
     * Date and time when the message was created.
     */
    val timestampCreated: Date
)


