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
 * This typealias specifies that attributes using it should refer to `OperationAttributes`.
 *
 * AttributeId is supposed to be `OperationAttribute.Label.id`
 */
typealias AttributeId = String

/**
 * This typealias specifies that attributes using might refer to `OperationAttributes`
 * and additional characters and might require additional parsing .
 *
 * Example might be `"${operation.date} - ${operation.place}"`
 */
typealias AttributeFormatted = String

/**
 * Detailed information about displaying operation data
 *
 * Contains prearranged styles for the operation attributes for the app to display
 */
data class Templates(
    val list: ListTemplate?,
    val detail: DetailTemplate?
) {

    /**
     * ListTemplate defines how the operation should look in the list (active operations, history)
     *
     * List cell usually contains header, title, message(subtitle) and image
     */
    data class ListTemplate(
        val style: String?,
        val header: AttributeFormatted?,
        val title: AttributeFormatted?,
        val message: AttributeFormatted?,
        val image: AttributeId?
    )

    /**
     * DetailTemplate defines how the operation details should appear.
     *
     * Each operation can be divided into sections with multiple cells.
     * Attributes not mentioned in the `DetailTemplate` should be displayed without custom styling.
     */
    data class DetailTemplate(
        val style: String?,
        val showTitleAndMessage: Boolean?,
        val sections: List<Section>?
    ) {

        /**
         * Operation data can be divided into sections
         */
        data class Section(
            val style: String?,
            val title: AttributeId?,
            val cells: List<Cell>?
        ) {

            /**
             * Each section can have multiple cells of data
             */
            data class Cell(
                val name: AttributeId,
                val style: String?,
                val visibleTitle: Boolean?,
                val canCopy: Boolean?,
                val collapsable: Collapsable?
            ) {

                enum class Collapsable {
                    NO,
                    COLLAPSED,
                    YES
                }
            }
        }
    }
}
