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

import com.google.gson.annotations.SerializedName

/**
 * Abstract element rendered inside a [PreApprovalScreen].
 * Use [PreApprovalElementTypeAdapter] with Gson to deserialize arrays of mixed element types.
 */
open class PreApprovalElement(

    /** Unique identifier of the element. */
    @SerializedName("id")
    val id: String?,

    /** Element type. */
    @SerializedName("type")
    val type: Type,

    /** Textual content. */
    @SerializedName("text")
    val text: String?
) {
    /**
     * Type of the element. Based on this type, a proper subclass
     * will be chosen during deserialization.
     */
    enum class Type {

        /** Basic list row. Retype the class to [PreApprovalElementListItem] */
        @SerializedName("LIST_ITEM")
        LIST_ITEM,

        /** Highlighted alert box [PreApprovalElementAlert] */
        @SerializedName("ALERT")
        ALERT,

        /** Action button with action [PreApprovalElementButton] */
        @SerializedName("BUTTON")
        BUTTON,
        UNKNOWN
    }

    /** Supported alert styles. */
    enum class Style {
        @SerializedName("INFO")
        INFO,

        @SerializedName("WARNING")
        WARNING,

        @SerializedName("DANGER")
        DANGER
    }
}

/** Button element (action + actionSettings + optional href + text). */
class PreApprovalElementButton(

    /** Unique identifier of the element. */
    id: String? = null,

    /** Supported button actions. */
    @SerializedName("action")
    val action: ButtonAction? = null,

    /** Custom extended behavior or secondary action for the button, for example "REJECT". */
    @SerializedName("actionSettings")
    val actionSettings: String? = null,

    /** URL / resource reference. */
    @SerializedName("href")
    val href: String? = null,

    /** Textual content. */
    text: String? = null
) : PreApprovalElement(id, Type.BUTTON, text) {
    enum class ButtonAction {

        @SerializedName("LINK")
        LINK,

        @SerializedName("MAIL")
        MAIL,

        @SerializedName("PHONE")
        PHONE
    }
}

/** Alert element (highlighted message with style). */
class PreApprovalElementAlert(

    /** Unique identifier of the element. */
    id: String? = null,

    /** Visual style for the alert. */
    @SerializedName("style")
    val style: PreApprovalElement.Style? = null,

    /** Textual content. */
    text: String? = null
) : PreApprovalElement(id, Type.ALERT, text)

/** List item (one row with optional icon + text + optional style). */
class PreApprovalElementListItem(

    /** Unique identifier of the element. */
    id: String? = null,

    /** Visual style for the list item. */
    @SerializedName("style")
    val style: PreApprovalElement.Style? = null,

    /** Icon name (asset identifier). */
    @SerializedName("icon")
    val icon: String? = null,

    /** Textual content. */
    text: String? = null
) : PreApprovalElement(id, Type.LIST_ITEM, text)
