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

package com.wultra.android.mtokensdk.api.operation.model

/**
 * Value of the `AttributeId` is referencing an existing `WMTOperationAttribute` by `WMTOperationAttribute.AttributeLabel.id`
 */
typealias AttributeId = String

/**
 * Value of the `AttributeFormatted` typealias contains placeholders for operation attributes,
 * which are specified using the syntax `${operation.attribute}`.
 *
 * Example might be `"${operation.date} - ${operation.place}"`
 * Placeholders in `AttributeFormatted` need to be parsed and replaced with actual attribute values.
 */
typealias AttributeFormatted = String

/**
 * Detailed information about displaying operation data
 *
 * Contains prearranged styles for the operation attributes for the app to display
 */
data class Templates(

    /**
     * How the operation should look like in the list of operations
     */
    val list: ListTemplate?,

    /**
     * How the operation detail should look like when viewed individually.
     */
    val detail: DetailTemplate?
) {

    /**
     * ListTemplate defines how the operation should look in the list (active operations, history)
     *
     * List cell usually contains header, title, message(subtitle) and image
     */
    data class ListTemplate(

        /** Prearranged name which can be processed by the app */
        val style: String?,

        /** Attribute which will be used for the header */
        val header: AttributeFormatted?,

        /** Attribute which will be used for the title */
        val title: AttributeFormatted?,

        /** Attribute which will be used for the message */
        val message: AttributeFormatted?,

        /** Attribute which will be used for the image */
        val image: AttributeId?
    )

    /**
     * DetailTemplate defines how the operation details should appear.
     *
     * Each operation can be divided into sections with multiple cells.
     * Attributes not mentioned in the `DetailTemplate` should be displayed without custom styling.
     */
    data class DetailTemplate(

        /** Predefined style name that can be processed by the app to customize the overall look of the operation. */
        val style: String?,

        /** Indicates if the header should be created from form data (title, message, image) or customized for a specific operation */
        val showTitleAndMessage: Boolean?,

        /** Sections of the operation data. */
        val sections: List<Section>?
    ) {

        /**
         * Operation data can be divided into sections
         */
        data class Section(

            /** Prearranged name which can be processed by the app to customize the section */
            val style: String?,

            /** Attribute for section title */
            val title: AttributeId?,

            /** Each section can have multiple cells of data */
            val cells: List<Cell>?
        ) {

            /**
             * Each section can have multiple cells of data
             */
            data class Cell(

                /** Which attribute shall be used */
                val name: AttributeId,

                /** Prearranged name which can be processed by the app to customize the cell */
                val style: String?,

                /** Should be the title visible or hidden */
                val visibleTitle: Boolean?,

                /** Should be the content copyable */
                val canCopy: Boolean?,

                /** Define if the cell should be collapsable */
                val collapsable: Collapsable?,

                /** If value should be centered */
                val centered: Boolean?
            ) {

                enum class Collapsable {

                    /** The cell should not be collapsable */
                    NO,

                    /** The cell should be collapsable and in collapsed state */
                    COLLAPSED,

                    /** The cell should be collapsable and in expanded state */
                    YES
                }
            }
        }
    }
}
