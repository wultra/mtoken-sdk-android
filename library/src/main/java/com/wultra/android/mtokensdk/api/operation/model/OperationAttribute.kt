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

package com.wultra.android.mtokensdk.api.operation.model

import java.math.BigDecimal

/**
 * Base model class of operation attribute hierarchy.
 * Operation Attribute can be visualized as "1 row in operation screen"
 * Every type of the attribute has it's own strongly typed implementation based on its [type]
 */
open class Attribute(

        /**
         * Type of the operation
         */
        val type: Type,

        /**
         * Label for the value
         */
        val label: Label) {

    enum class Type {
        AMOUNT,
        KEY_VALUE,
        NOTE,
        HEADING,
        PARTY_INFO,
        AMOUNT_CONVERSION,
        IMAGE,
        UNKNOWN
    }

    /**
     * Attribute label serves as a UI heading for the attribute
     */
    data class Label(
            /**
             * ID (type) of the label. This is highly depended on the backend
             * and can be used to change the appearance of the label
             */
            val id: String,

            /**
             * Label value
             */
            val value: String)
}

/**
 * Amount attribute is 1 row in operation that represents "Payment Amount"
 */
class AmountAttribute(
        /**
         * Payment amount
         */
        val amount: BigDecimal,

        /**
         * Currency
         */
        val currency: String,

        /**
         * Formatted amount for presentation.
         *
         * This property will be properly formatted based on the response language.
         * For example when amount is 100 and the acceptLanguage is "cs" for czech,
         * he amountFormatted will be "100,00".
         */
        val amountFormatted: String?,

        /**
         * Formatted currency to the locale based on acceptLanguage.
         *
         * For example when the currency is CZK, this property will be "Kč"
         */
        val currencyFormatted: String?,

        label: Label) : Attribute(Type.AMOUNT, label)

/**
 * Attribute that describes generic key-value row to display
 */
class KeyValueAttribute(

        /**
         * Value of the attribute
         */
        val value: String,

        label: Label) : Attribute(Type.KEY_VALUE, label)

/**
 * Attribute that describes note, that should be handled as "long text message"
 */
class NoteAttribute(

        /**
         * Note value
         */
        val note: String,

        label: Label) : Attribute(Type.NOTE, label)

/**
 * Heading. This attribute has no value. It only acts as a "section separator"
 */
class HeadingAttribute(label: Label) : Attribute(Type.HEADING, label)

/**
 * Third party info is for providing structured information about third party data.
 *
 * This can be used for example when you're approving payment in some retail eshop,
 * in such case, information about the eshop will be filled here.
 */
class PartyInfoAttribute(
        /**
         * Information about the 3rd party info
         */
        val partyInfo: PartyInfo,

        label: Label) : Attribute(Type.PARTY_INFO, label) {

    /**
     * 3rd party retailer information
     */
    class PartyInfo(map: Map<String, String>) {
        /**
         * URL address to the logo image
         */
        val logoUrl: String by map

        /**
         * Name of the retailer
         */
        val name: String by map

        /**
         * Description of the retailer
         */
        val description: String by map

        /**
         * Retailer website
         */
        val websiteUrl: String by map
    }
}

/**
 * Conversion attribute is 1 row in operation, that represents "Money Conversion"
 */
class ConversionAttribute(
    /**
     * If the conversion is dynamic and the application should refresh it periodically.
     *
     * This is just a hint for the application UI. This SDK does not offer feature to periodically
     * refresh conversion rate.
     */
    val dynamic: Boolean,
    /** Source amount */
    val source: Money,
    /** Target amount */
    val target: Money,
    label: Label) : Attribute(Type.AMOUNT_CONVERSION, label) {

    data class Money(

        /**
         * Payment amount
         *
         * Amount might not be precise (due to floating point conversion during deserialization from json)
         * use amountFormatted property instead when available
         */
        val amount: BigDecimal,

        /** Currency */
        val currency: String,

        /**
         * Formatted amount for presentation.
         *
         * This property will be properly formatted based on the response language.
         * For example when amount is 100 and the acceptLanguage is "cs" for czech,
         * the amountFormatted will be "100,00".
         */
        val amountFormatted: String?,

        /**
         * Formatted currency to the locale based on acceptLanguage
         *
         * For example when the currency is CZK, this property will be "Kč"
         */
        val currencyFormatted: String?
    )
}

/**
 * Image row in the operation. If the originalUrl is present, it should be "clickable".
 */
class ImageAttribute(
    /** Image thumbnail url to the public internet */
    val thumbnailUrl: String,

    /** Full-size image that should be displayed on thumbnail click (when not null). Url to the public internet */
    val originalUrl: String?,

    label: Label): Attribute(Type.IMAGE, label)