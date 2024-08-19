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
import com.wultra.android.mtokensdk.api.operation.model.ImageAttribute
import com.wultra.android.mtokensdk.api.operation.model.KeyValueAttribute
import com.wultra.android.mtokensdk.api.operation.model.NoteAttribute
import com.wultra.android.mtokensdk.api.operation.model.Templates
import com.wultra.android.mtokensdk.api.operation.model.UserOperation
import com.wultra.android.mtokensdk.log.WMTLogger

/**
 * `TemplateDetailVisual` holds the visual data for displaying a detailed view of a user operation.
 */
data class TemplateDetailVisual(

    /** Predefined style of the whole operation detail to which the app can react and adjust the operation visual */
    val style: String?,

    /** An array of `UserOperationVisualSection` defining the sections of the detailed view. */
    val sections: List<UserOperationVisualSection>
)

/**
 * This class defines one section in the detailed view of a user operation.
 */
data class UserOperationVisualSection(

    /** Predefined style of the section to which the app can react and adjust the operation visual */
    val style: String? = null,

    /** The title value for the section */
    val title: String? = null,

    /** An array of cells with `FormData` header and message or visual cells based on `OperationAttributes` */
    val cells: List<UserOperationVisualCell>
)

/**
 * An interface for visual cells in a user operation's detailed view.
 */
interface UserOperationVisualCell

/**
 * `UserOperationHeaderVisualCell` contains a header in a user operation's detail header view.
 *
 * This data class is used to distinguish between the default header section and custom `OperationAttribute` sections.
 */
data class UserOperationHeaderVisualCell(

    /** This value corresponds to `FormData.title` */
    val value: String
) : UserOperationVisualCell

/**
 * `UserOperationMessageVisualCell` is a message cell in a user operation's header view.
 *
 * This data class is used within default header section and is used to distinguished from custom `OperationAttribute` sections.
 */
data class UserOperationMessageVisualCell(

    /** This value corresponds to `FormData.message` */
    val value: String
) : UserOperationVisualCell

/**
 * `UserOperationHeadingVisualCell` defines a heading cell in a user operation's detailed view.
 */
data class UserOperationHeadingVisualCell(

    /** Single highlighted text used as a section heading */
    val header: String,

    /** Predefined style of the section cell, app shall react to it and should change the visual of the cell */
    val style: String? = null,

    /** The source user operation attribute. */
    val attribute: Attribute,

    /** The template the cell was made from. */
    val cellTemplate: Templates.DetailTemplate.Section.Cell? = null
) : UserOperationVisualCell

/**
 * `UserOperationValueAttributeVisualCell` defines a value attribute cell in a user operation's detailed view.
 */
data class UserOperationValueAttributeVisualCell(

    /** The header text value */
    val header: String,

    /** The text value preformatted for the cell (if the preformatted value isn't sufficient, the value from the attribute can be used) */
    val defaultFormattedStringValue: String,

    /** Predefined style of the section cell, app shall react to it and should change the visual of the cell */
    val style: String? = null,

    /** /// The source user operation attribute. */
    val attribute: Attribute,

    /** The template the cell was made from. */
    val cellTemplate: Templates.DetailTemplate.Section.Cell? = null
) : UserOperationVisualCell

/**
 * `UserOperationImageVisualCell` defines an image cell in a user operation's detailed view.
 */
data class UserOperationImageVisualCell(

    /** The URL of the thumbnail image */
    val urlThumbnail: String,

    /** The URL of the full size image */
    val urlFull: String? = null,

    /** Predefined style of the section cell, app shall react to it and should change the visual of the cell */
    val style: String? = null,

    /** The source user operation attribute. */
    val attribute: ImageAttribute,

    /** The template the cell was made from. */
    val cellTemplate: Templates.DetailTemplate.Section.Cell? = null
) : UserOperationVisualCell

/**
 * Extension function to prepare the visual representation for the given `UserOperation` in a detailed view.
 */
fun UserOperation.prepareVisualDetail(): TemplateDetailVisual {
    val detailTemplate = this.ui?.templates?.detail

    return if (detailTemplate == null) {
        val sections = mutableListOf(createDefaultHeaderVisual())
        if (formData.attributes.isNotEmpty()) {
            sections.add(UserOperationVisualSection(cells = formData.attributes.getRemainingCells()))
        }
        TemplateDetailVisual(style = null, sections = sections)
    } else {
        createDetailVisual(detailTemplate)
    }
}

private fun UserOperation.createDefaultHeaderVisual(): UserOperationVisualSection {
    val defaultHeaderCell = UserOperationHeaderVisualCell(value = this.formData.title)
    val defaultMessageCell = UserOperationMessageVisualCell(value = this.formData.message)

    return UserOperationVisualSection(
        style = null,
        title = null,
        cells = listOf(defaultHeaderCell, defaultMessageCell)
    )
}

private fun UserOperation.createDetailVisual(detailTemplate: Templates.DetailTemplate): TemplateDetailVisual {
    val attributes = this.formData.attributes.toMutableList()

    val sections = mutableListOf<UserOperationVisualSection>()

    if (detailTemplate.showTitleAndMessage == false) {
        sections.addAll(attributes.popCellsFromSections(detailTemplate.sections))
        sections.add(UserOperationVisualSection(cells = attributes.getRemainingCells()))
    } else {
        sections.add(createDefaultHeaderVisual())
        sections.addAll(attributes.popCellsFromSections(detailTemplate.sections))
        sections.add(UserOperationVisualSection(cells = attributes.getRemainingCells()))
    }

    return TemplateDetailVisual(style = detailTemplate.style, sections = sections)
}

private fun MutableList<Attribute>.popCellsFromSections(
    sections: List<Templates.DetailTemplate.Section>?
): List<UserOperationVisualSection> {
    return sections?.map { popCellsFromSection(it) } ?: emptyList()
}

private fun MutableList<Attribute>.popCellsFromSection(
    section: Templates.DetailTemplate.Section
): UserOperationVisualSection {
    return UserOperationVisualSection(
        style = section.style,
        title = popAttribute(section.title)?.label?.value,
        cells = section.cells?.mapNotNull { createCellFromTemplateCell(it) } ?: emptyList()
    )
}

private fun MutableList<Attribute>.popAttribute(id: String?): Attribute? {
    id?.let {
        val index = indexOfFirst { it.label.id == id }
        return if (index != -1) removeAt(index) else null
    }
    return null
}

private fun MutableList<Attribute>.createCellFromTemplateCell(
    templateCell: Templates.DetailTemplate.Section.Cell
): UserOperationVisualCell? {
    val attr = popAttribute(templateCell.name) ?: return null.also {
        WMTLogger.w("Template Attribute '${templateCell.name}', not found in FormData Attributes")
    }
    return createCell(attr, templateCell)
}

private fun List<Attribute>.getRemainingCells(): List<UserOperationVisualCell> {
    return mapNotNull { createCell(it) }
}

private fun createCell(
    attr: Attribute,
    templateCell: Templates.DetailTemplate.Section.Cell? = null
): UserOperationVisualCell? {
    return when (attr.type) {
        Attribute.Type.AMOUNT -> {
            val amount = attr as? AmountAttribute ?: return null
            UserOperationValueAttributeVisualCell(
                header = attr.label.value,
                defaultFormattedStringValue = amount.valueFormatted
                    ?: "${amount.amountFormatted} ${amount.currencyFormatted}",
                style = templateCell?.style,
                attribute = attr,
                cellTemplate = templateCell
            )
        }
        Attribute.Type.AMOUNT_CONVERSION -> {
            val conversion = attr as? ConversionAttribute ?: return null
            val sourceValue = conversion.source.valueFormatted
                ?: "${conversion.source.amountFormatted} ${conversion.source.currencyFormatted}"
            val targetValue = conversion.target.valueFormatted
                ?: "${conversion.target.amountFormatted} ${conversion.target.currencyFormatted}"
            UserOperationValueAttributeVisualCell(
                header = attr.label.value,
                defaultFormattedStringValue = "$sourceValue → $targetValue",
                style = templateCell?.style,
                attribute = attr,
                cellTemplate = templateCell
            )
        }
        Attribute.Type.KEY_VALUE -> {
            val keyValue = attr as? KeyValueAttribute ?: return null
            UserOperationValueAttributeVisualCell(
                header = attr.label.value,
                defaultFormattedStringValue = keyValue.value,
                style = templateCell?.style,
                attribute = attr,
                cellTemplate = templateCell
            )
        }
        Attribute.Type.NOTE -> {
            val note = attr as? NoteAttribute ?: return null
            UserOperationValueAttributeVisualCell(
                header = attr.label.value,
                defaultFormattedStringValue = note.note,
                style = templateCell?.style,
                attribute = attr,
                cellTemplate = templateCell
            )
        }
        Attribute.Type.IMAGE -> {
            val image = attr as? ImageAttribute ?: return null
            UserOperationImageVisualCell(
                urlThumbnail = image.thumbnailUrl,
                urlFull = image.originalUrl,
                style = templateCell?.style,
                attribute = image,
                cellTemplate = templateCell
            )
        }
        Attribute.Type.HEADING -> {
            UserOperationHeadingVisualCell(
                header = attr.label.value,
                style = templateCell?.style,
                attribute = attr,
                cellTemplate = templateCell
            )
        }
        else -> {
            WMTLogger.w("Using unsupported Attribute in Templates")
            null
        }
    }
}
