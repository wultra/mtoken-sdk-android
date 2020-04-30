/*
 * Copyright (c) 2020, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package com.wultra.android.mtokensdk.api.operation.model

import java.math.BigDecimal

/**
 * Base model class of operation attribute hierarchy.
 */
sealed class Attribute(val type: Type) {

    abstract val id: String?
    abstract val label: String?

    enum class Type(val typeInt: Int) {
        AMOUNT(0),
        KEY_VALUE(1),
        NOTE(2),
        HEADING(3),
        PARTY_INFO(4)
    }
}

/**
 * Model class for AMOUNT operation attribute.
 */
data class AmountAttribute(override val id: String?,
                           override val label: String?,
                           val amount: BigDecimal?,
                           val currency: String?,
                           val amountFormatted: String?,
                           val currencyFormatted: String?) : Attribute(Type.AMOUNT)

/**
 * Model class for KEY_VALUE operation attribute.
 */
data class KeyValueAttribute(override val id: String?,
                             override val label: String?,
                             val value: String?) : Attribute(Type.KEY_VALUE)

/**
 * Model class for NOTE operation attribute.
 */
data class NoteAttribute(override val id: String?,
                         override val label: String?,
                         val note: String?) : Attribute(Type.NOTE)

/**
 * Model class for HEADING operation attribute.
 */
data class HeadingAttribute(override val id: String?,
                            override val label: String?) : Attribute(Type.HEADING)

/**
 * Model class for PARTY_INFO operation attribute.
 */
data class PartyInfoAttribute(override val id: String?,
                              override val label: String?,
                              val partyInfo: PartyInfo) : Attribute(Type.PARTY_INFO)

/**
 * Model class for PARTY_INFO attribute data.
 */
data class PartyInfo(val map: Map<String, String>) {
    val logoUrl: String by map
    val name: String by map
    val description: String by map
    val websiteUrl: String by map
}