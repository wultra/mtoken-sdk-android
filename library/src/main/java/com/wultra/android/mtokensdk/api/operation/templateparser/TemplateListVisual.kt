/*
 * Copyright 2024 Wultra s.r.o.
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

package com.wultra.android.mtokensdk.api.operation.templateparser

import com.wultra.android.mtokensdk.api.operation.model.AmountAttribute
import com.wultra.android.mtokensdk.api.operation.model.Attribute
import com.wultra.android.mtokensdk.api.operation.model.ConversionAttribute
import com.wultra.android.mtokensdk.api.operation.model.HeadingAttribute
import com.wultra.android.mtokensdk.api.operation.model.ImageAttribute
import com.wultra.android.mtokensdk.api.operation.model.KeyValueAttribute
import com.wultra.android.mtokensdk.api.operation.model.NoteAttribute
import com.wultra.android.mtokensdk.api.operation.model.Templates
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.log.WMTLogger
import java.net.URL

/**
 * `TemplateListVisual` holds the visual data for displaying a user operation in a list view (RecyclerView/ListView).
 */
data class TemplateListVisual(
    val header: String? = null,
    val title: String? = null,
    val message: String? = null,
    val style: String? = null,
    val thumbnailImageURL: URL? = null,
    val template: Templates.ListTemplate? = null
)

/**
 * Extension function to prepare the visual representation for the given `UserOperation` in a list view.
 */
fun UserOperation.prepareVisualListDetail(): TemplateListVisual {
    val listTemplate = this.ui?.templates?.list
    val attributes = this.formData.attributes
    val headerAttr = listTemplate?.header?.replacePlaceholders(attributes)

    val title: String? = listTemplate?.title?.replacePlaceholders(attributes)
        ?: if (this.formData.message.isNotEmpty()) this.formData.title else null

    val message: String? = listTemplate?.message?.replacePlaceholders(attributes)
        ?: if (this.formData.message.isNotEmpty()) this.formData.message else null

    val imageUrl: URL? = listTemplate?.image?.let { imgAttr ->
        this.formData.attributes
            .filterIsInstance<ImageAttribute>()
            .firstOrNull { it.label.id == imgAttr }
            ?.let { URL(it.thumbnailUrl) }
    }

    return TemplateListVisual(
        header = headerAttr,
        title = title,
        message = message,
        style = this.ui?.templates?.list?.style,
        thumbnailImageURL = imageUrl,
        template = listTemplate
    )
}

/**
 * Extension function to replace placeholders in the template with actual values.
 */
fun String.replacePlaceholders(attributes: List<Attribute>): String? {
    var result = this

    val placeholders = extractPlaceholders()
    placeholders?.forEach { placeholder ->
        val value = findAttributeValue(placeholder, attributes)
        if (value != null) {
            result = result.replace("\${$placeholder}", value)
        } else {
            WMTLogger.d("Placeholder Attribute: $placeholder not found.")
            return null
        }
    }
    return result
}

/**
 * Extracts placeholders from the string.
 */
private fun String.extractPlaceholders(): List<String>? {
    return try {
        val regex = Regex("""\$\{(.*?)\}""")
        regex.findAll(this).map { it.groupValues[1] }.toList()
    } catch (e: Exception) {
        WMTLogger.w("Error creating regex: $e in TemplatesListParser.")
        null
    }
}

/**
 * Finds the attribute value for a given attribute ID from the attributes list.
 */
private fun findAttributeValue(attributeId: String, attributes: List<Attribute>): String? {
    return attributes.firstOrNull { it.label.id == attributeId }?.let { attribute ->
        when (attribute.type) {
            Attribute.Type.AMOUNT -> {
                val attr = attribute as? AmountAttribute
                attr?.valueFormatted ?: "${attr?.amountFormatted} ${attr?.currencyFormatted}"
            }
            Attribute.Type.AMOUNT_CONVERSION -> {
                val attr = attribute as? ConversionAttribute
                if (attr != null) {
                    val sourceValue = attr.source.valueFormatted ?: "${attr.source.amountFormatted} ${attr.source.currencyFormatted}"
                    val targetValue = attr.target.valueFormatted ?: "${attr.target.amountFormatted} ${attr.target.currencyFormatted}"
                    "$sourceValue → $targetValue"
                } else {
                    null
                }
            }
            Attribute.Type.KEY_VALUE -> (attribute as? KeyValueAttribute)?.value
            Attribute.Type.NOTE -> (attribute as? NoteAttribute)?.note
            Attribute.Type.HEADING -> (attribute as? HeadingAttribute)?.label?.value
            else -> null
        }
    }
}
