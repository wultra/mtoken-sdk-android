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

package com.wultra.android.mtokensdk.api.operation

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.wultra.android.mtokensdk.api.operation.model.Templates
import com.wultra.android.mtokensdk.log.WMTLogger
import java.lang.reflect.Type

/**
 * Custom Templates deserializer
 */

internal class TemplatesDeserializer : JsonDeserializer<Templates> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Templates {
        val jsonObject = json.asJsonObject

        val listTemplate = jsonObject.get("list")?.let { listElement ->
            try {
                context.deserialize<Templates.ListTemplate>(listElement, Templates.ListTemplate::class.java)
            } catch (e: Exception) {
                WMTLogger.e("Failed to deserialize ListTemplate - ${e.message}")
                null
            }
        }

        val detailTemplate = jsonObject.get("detail")?.let { detailElement ->
            try {
                context.deserialize<Templates.DetailTemplate>(detailElement, Templates.DetailTemplate::class.java)
            } catch (e: Exception) {
                WMTLogger.e("Failed to deserialize DetailTemplate - ${e.message}")
                null
            }
        }

        return Templates(listTemplate, detailTemplate)
    }
}

internal class ListTemplateDeserializer : JsonDeserializer<Templates.ListTemplate> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Templates.ListTemplate {
        val jsonObject = json.asJsonObject

        val style: String? = jsonObject.get("style")?.asStringWithLogging("ListTemplate.style")
        val header: String? = jsonObject.get("header")?.asStringWithLogging("ListTemplate.header")
        val title: String? = jsonObject.get("title")?.asStringWithLogging("ListTemplate.title")
        val message: String? = jsonObject.get("message")?.asStringWithLogging("ListTemplate.message")
        val image: String? = jsonObject.get("image")?.asStringWithLogging("ListTemplate.image")

        return Templates.ListTemplate(style, header, title, message, image)
    }
}

internal class DetailTemplateDeserializer : JsonDeserializer<Templates.DetailTemplate> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Templates.DetailTemplate {
        val jsonObject = json.asJsonObject

        val style: String? = jsonObject.get("style")?.asStringWithLogging("DetailTemplate.style")
        val showTitleAndMessage: Boolean? = jsonObject.get("showTitleAndMessage")?.asBooleanWithLogging("DetailTemplate.showTitleAndMessage")

        val sections: List<Templates.DetailTemplate.Section>? = try {
            val sectionsElement = jsonObject.get("sections")
            val sectionType = object : TypeToken<List<Templates.DetailTemplate.Section>>() {}.type
            context.deserialize<List<Templates.DetailTemplate.Section>>(sectionsElement, sectionType)
        } catch (e: Exception) {
            WMTLogger.e("Failed to decode 'DetailTemplate.sections' - ${e.message}, setting to null")
            null
        }

        return Templates.DetailTemplate(style, showTitleAndMessage, sections)
    }
}

internal class SectionDeserializer : JsonDeserializer<Templates.DetailTemplate.Section> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Templates.DetailTemplate.Section {
        val jsonObject = json.asJsonObject

        val style: String? = jsonObject.get("style")?.asStringWithLogging("ListTemplate.Section.style")
        val title: String? = jsonObject.get("title")?.asStringWithLogging("ListTemplate.Section.title")


        val cells: List<Templates.DetailTemplate.Section.Cell>? = jsonObject.get("cells")?.let { cellsElement ->
            try {
                if (cellsElement.isJsonArray) {
                    val cellList = mutableListOf<Templates.DetailTemplate.Section.Cell>()
                    val jsonArray = cellsElement.asJsonArray
                    jsonArray.forEach { cellElement ->
                        try {
                            val cell = context.deserialize<Templates.DetailTemplate.Section.Cell>(cellElement, Templates.DetailTemplate.Section.Cell::class.java)
                            cellList.add(cell)
                        } catch (e: Exception) {
                            WMTLogger.e("Failed to decode cell in DetailTemplate.Section.cells - ${e.message}")
                        }
                    }
                    cellList
                } else {
                    WMTLogger.e("Failed to decode 'DetailTemplate.Sections.cells' - Expected a JSON array, setting to null")
                    null
                }
            } catch (e: Exception) {
                WMTLogger.e("Failed to decode DetailTemplate.Section.cells - ${e.message}, setting to null")
                null
            }
        }


        return Templates.DetailTemplate.Section(style, title, cells)
    }
}

internal class CellDeserializer : JsonDeserializer<Templates.DetailTemplate.Section.Cell> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Templates.DetailTemplate.Section.Cell {
        val jsonObject = json.asJsonObject

        val name = jsonObject.get("name").asString

        val style: String? = jsonObject.get("style")?.asStringWithLogging("DetailTemplate.Section.Cell.style")
        val visibleTitle: Boolean? = jsonObject.get("visibleTitle")?.asBooleanWithLogging("DetailTemplate.Section.Cell.visibleTitle")
        val canCopy: Boolean? = jsonObject.get("canCopy")?.asBooleanWithLogging("DetailTemplate.Section.Cell.canCopy")

        val collapsable = jsonObject.get("collapsable")?.asStringWithLogging("DetailTemplate.Section.Cell.collapsable")?.let {
            Templates.DetailTemplate.Section.Cell.Collapsable.valueOf(it)
        }

        val centered: Boolean? = jsonObject.get("centered")?.asBooleanWithLogging("DetailTemplate.Section.Cell.centered")

        return Templates.DetailTemplate.Section.Cell(name, style, visibleTitle, canCopy, collapsable, centered)
    }
}

private fun JsonElement.asStringWithLogging(fieldName: String): String? {
    try {
        if (this.isJsonNull) {
            return null
        }

        if (this.isJsonPrimitive && !this.asJsonPrimitive.isString) {
            WMTLogger.e("Failed to decode '$fieldName' - $this is not a String, setting to null")
            return null
        }

        return this.asString
    } catch (e: Exception) {
        WMTLogger.e("Failed to decode '$fieldName' - ${e.message}, setting to null")
        return null
    }
}

private fun JsonElement.asBooleanWithLogging(fieldName: String): Boolean? {
    try {
        if (this.isJsonNull) {
            return null
        }

        if (this.isJsonPrimitive && !this.asJsonPrimitive.isBoolean) {
            WMTLogger.e("Failed to decode '$fieldName' - $this is not a Boolean, setting to null")
            return null
        }

        return this.asBoolean
    } catch (e: Exception) {
        WMTLogger.e("Failed to decode '$fieldName' - ${e.message}, setting to null")
        return null
    }
}
