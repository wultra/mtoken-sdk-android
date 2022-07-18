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

import java.util.Properties
import java.io.FileInputStream
import com.android.build.gradle.internal.dsl.DefaultConfig

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(30)
        versionCode = 1
        versionName = properties["VERSION_NAME"] as String
        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
        loadInstrumentationTestConfigProperties(project, this)
    }

    buildTypes {
        getByName("debug") {

        }
        getByName("release") {
            minifyEnabled(false)
            consumerProguardFiles("consumer-proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        suppressWarnings = false
    }
}

dependencies {
    // Bundled
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Constants.kotlinVersion}")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    // DO NOT UPGRADE ABOVE 3.12.X! Version 3.12 is the last version supporting TLS 1 and 1.1
    // If upgraded, the app will crash on android 4.4
    implementation("com.squareup.okhttp3:okhttp:3.12.12")
    implementation("com.wultra.android.powerauth:powerauth-networking:1.0.2")

    // Dependencies
    compileOnly("com.wultra.android.powerauth:powerauth-sdk:1.7.0")
    compileOnly("io.getlime.core:rest-model-base:1.2.0")

    // TestDependencies
    testImplementation("junit:junit:4.13.1")

    // Android tests
    androidTestImplementation("com.wultra.android.powerauth:powerauth-sdk:1.7.0")
    androidTestImplementation("com.wultra.android.powerauth:powerauth-networking:1.0.2")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test:core:1.4.0")
}

// Load properties for instrumentation tests.
fun loadInstrumentationTestConfigProperties(project: Project, defaultConfig: DefaultConfig) {
    val configsRoot = File("${project.rootProject.projectDir}/configs")
    val configPropertiesFile = File(configsRoot, "integration-tests.properties")

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
            defaultConfig.testInstrumentationRunnerArgument(key, "${props[key]}")
        }
    } else {
        project.logger.warn("Loading properties error: Missing $configPropertiesFile")
    }
}

apply("android-release-aar.gradle")