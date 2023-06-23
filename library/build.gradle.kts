@file:Suppress("UnstableApiUsage")

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

plugins {
    id("com.android.library")
    kotlin("android")
}

android {

    namespace = "com.wultra.android.mtokensdk"

    compileSdk = Constants.Android.compileSdkVersion
    buildToolsVersion = Constants.Android.buildToolsVersion

    defaultConfig {
        minSdk = Constants.Android.minSdkVersion
        targetSdk = Constants.Android.targetSdkVersion

        // since Android Gradle Plugin 4.1.0
        // VERSION_CODE and VERSION_NAME are not generated for libraries
        configIntField("VERSION_CODE", 1)
        configStringField("VERSION_NAME", properties["VERSION_NAME"] as String)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        loadInstrumentationTestConfigProperties(project, this)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = Constants.Java.sourceCompatibility
        targetCompatibility = Constants.Java.targetCompatibility
        kotlinOptions {
            jvmTarget = Constants.Java.kotlinJvmTarget
            suppressWarnings = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // Custom ktlint script
    tasks.register("ktlint") {
        logger.lifecycle("ktlint")
        exec {
            commandLine = listOf("./../scripts/lint.sh", "--no-error")
        }
    }

    // Make ktlint run before build
    tasks.getByName("preBuild").dependsOn("ktlint")
}

dependencies {
    // Bundled
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Constants.BuildScript.kotlinVersion}")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    // DO NOT UPGRADE ABOVE 3.12.X! Version 3.12 is the last version supporting TLS 1 and 1.1
    // If upgraded, the app will crash on android 4.4
    implementation("com.squareup.okhttp3:okhttp:3.12.13")
    implementation("com.wultra.android.powerauth:powerauth-networking:1.2.0")

    // Dependencies
    compileOnly("com.wultra.android.powerauth:powerauth-sdk:1.7.8")
    compileOnly("io.getlime.core:rest-model-base:1.2.0")

    // TestDependencies
    testImplementation("junit:junit:4.13.2")

    // Android tests
    androidTestImplementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    androidTestImplementation("com.wultra.android.powerauth:powerauth-sdk:1.7.8")
    androidTestImplementation("com.wultra.android.powerauth:powerauth-networking:1.2.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
}

apply("android-release-aar.gradle")
