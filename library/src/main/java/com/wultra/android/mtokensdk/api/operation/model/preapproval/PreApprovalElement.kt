/*
 * Copyright 2025 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.api.operation.model.preapproval

/**
 * Abstract element rendered inside a [PreApprovalScreen].
 * Use [PreApprovalElementTypeAdapter] with Gson to deserialize arrays of mixed element types.
 */
open class PreApprovalElement(

    /** Unique identifier of the element. */
    val id: String?,

    /** Element type (LIST_ITEM, ALERT, BUTTON, or UNKNOWN). */
    val type: ElementType,

    /** Icon name (asset identifier). */
    val icon: String?,

    /** Textual content. */
    val text: String?
) {
    /**
     * Type of the element. Based on this type, a proper subclass
     * will be chosen during deserialization.
     */
    enum class ElementType {
        LIST_ITEM, // Basic list row with optional icon + text
        ALERT, // Highlighted alert box with style + text
        BUTTON, // Action button with action + optional href
        UNKNOWN // Forward-compat fallback
    }

    /** Supported alert styles. */
    enum class ElementStyle { INFO, WARNING, DANGER }
}

/** Button element (action + optional href + text). */
class PreApprovalElementButton(

    /** Unique identifier of the element. */
    id: String? = null,

    /** Supported button actions. */
    val action: ButtonAction? = null,

    /** URL / resource reference (for `LINK`, `MAIL`, `PHONE` actions). */
    val href: String? = null,

    /** Icon name (asset identifier). */
    icon: String? = null,

    /** Textual content. */
    text: String? = null
) : PreApprovalElement(id, ElementType.BUTTON, icon, text) {
    enum class ButtonAction { LINK, MAIL, PHONE }
}

/** Alert element (highlighted message with style). */
class PreApprovalElementAlert(

    /** Unique identifier of the element. */
    id: String? = null,

    /** Visual style for the alert. */
    val style: PreApprovalElement.ElementStyle? = null,

    /** Icon name (asset identifier). */
    icon: String? = null,

    /** Textual content. */
    text: String? = null
) : PreApprovalElement(id, ElementType.ALERT, icon, text)

/** List item (one row with optional icon + text + optional style). */
class PreApprovalElementListItem(

    /** Unique identifier of the element. */
    id: String? = null,

    /** Visual style for the list item. */
    val style: PreApprovalElement.ElementStyle? = null,

    /** Icon name (asset identifier). */
    icon: String? = null,

    /** Textual content. */
    text: String? = null
) : PreApprovalElement(id, ElementType.LIST_ITEM, icon, text)
