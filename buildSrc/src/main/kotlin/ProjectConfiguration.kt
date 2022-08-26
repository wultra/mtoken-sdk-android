/*
 * Copyright (c) 2022, Wultra s.r.o. (www.wultra.com).
 *
 * All rights reserved. This source code can be used only for purposes specified
 * by the given license contract signed by the rightful deputy of Wultra s.r.o.
 * This source code can be used only by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

import com.android.build.api.dsl.BaseFlavor
import com.android.build.api.dsl.DefaultConfig
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.*

fun wrapString(value: String?) = if (value != null) "\"${value}\"" else "null"

fun BaseFlavor.configStringField(name: String, value: String?) = buildConfigField("String", name, wrapString(value))
fun BaseFlavor.configBoolField(name: String, value: Boolean) = buildConfigField("boolean", name, "$value")
fun BaseFlavor.configLongField(name: String, value: Long) = buildConfigField("Long", name, "${value}L")
fun BaseFlavor.configIntField(name: String, value: Int) = buildConfigField("Integer", name, "$value")

// Load properties for instrumentation tests.
fun loadInstrumentationTestConfigProperties(project: Project, defaultConfig: DefaultConfig) {
    val configsRoot = File("${project.rootProject.projectDir}/configs")
    val defaultConfigFile = File(configsRoot, "integration-tests.properties")
    val privateConfigFile = File(configsRoot, "private-integration-tests.properties")
    val configPropertiesFile = if (privateConfigFile.canRead()) {
        privateConfigFile
    } else {
        defaultConfigFile
    }
    val instrumentationArguments = arrayOf(
        "tests.sdk.cloudServerUrl",
        "tests.sdk.cloudServerLogin",
        "tests.sdk.cloudServerPassword",
        "tests.sdk.cloudApplicationId",
        "tests.sdk.enrollmentServerUrl",
        "tests.sdk.operationsServerUrl",
        "tests.sdk.appKey",
        "tests.sdk.appSecret",
        "tests.sdk.masterServerPublicKey"
    )

    project.logger.info("LOADING_PROPERTIES Reading $configPropertiesFile")
    if (configPropertiesFile.canRead()) {
        val props = Properties()
        props.load(FileInputStream(configPropertiesFile))

        for (key in instrumentationArguments) {
            defaultConfig.testInstrumentationRunnerArguments[key] = "${props[key]}"
        }
    } else {
        project.logger.warn("Loading properties error: Missing $configPropertiesFile")
    }
}