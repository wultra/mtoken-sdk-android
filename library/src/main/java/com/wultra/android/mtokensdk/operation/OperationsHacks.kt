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

package com.wultra.android.mtokensdk.operation

import android.os.Build
import java.util.*


/**
 * Returns a well-formed IETF BCP 47 language tag representing
 * this locale.
 */
fun Locale.toBcp47LanguageTag(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return this.toLanguageTag()
    }

    val sep = '-'       // we will use a dash as per BCP 47
    var language = this.getLanguage()
    var region = this.getCountry()
    var variant = this.getVariant()

    // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
    // this goes before the string matching since "NY" wont pass the variant checks
    if (language == "no" && region == "NO" && variant == "NY") {
        language = "nn"
        region = "NO"
        variant = ""
    }

    if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}".toRegex())) {
        language = "und"       // Follow the Locale#toLanguageTag() implementation
        // which says to return "und" for Undetermined
    } else if (language == "iw") {
        language = "he"        // correct deprecated "Hebrew"
    } else if (language == "in") {
        language = "id"        // correct deprecated "Indonesian"
    } else if (language == "ji") {
        language = "yi"        // correct deprecated "Yiddish"
    }

    // ensure valid country code, if not well formed, it's omitted
    if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}".toRegex())) {
        region = ""
    }

    // variant subtags that begin with a letter must be at least 5 characters long
    if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}".toRegex())) {
        variant = ""
    }

    val bcp47Tag = StringBuilder(language)
    if (!region.isEmpty()) {
        bcp47Tag.append(sep).append(region)
    }
    if (!variant.isEmpty()) {
        bcp47Tag.append(sep).append(variant)
    }

    return bcp47Tag.toString()
}