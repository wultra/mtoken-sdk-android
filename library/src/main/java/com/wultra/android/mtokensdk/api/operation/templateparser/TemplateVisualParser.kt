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

import com.wultra.android.mtokensdk.api.operation.model.UserOperation

/**
 * This is a utility class responsible for preparing visual representations of `UserOperation`.
 *
 * It generates visual data for both list and detailed views of the operations from `OperationFormData` and its `OperationAttribute`.
 * The visual data are created based on the structure of the `Templates`.
 */
class TemplateVisualParser {

    companion object {

        /**
         * Prepares the visual representation for the given `UserOperation` in a list view.
         * @param operation The user operation to prepare the visual data for.
         * @return A `TemplateListVisual` instance containing the visual data.
         */
        @JvmStatic
        fun prepareForList(operation: UserOperation): TemplateListVisual {
            return operation.prepareVisualListDetail()
        }

        /**
         * Prepares the visual representation for a detail view of the given `UserOperation`.
         * @param operation The user operation to prepare the visual data for.
         * @return A `TemplateDetailVisual` instance containing the visual data.
         */
        @JvmStatic
        fun prepareForDetail(operation: UserOperation): TemplateDetailVisual {
            return operation.prepareVisualDetail()
        }
    }
}
