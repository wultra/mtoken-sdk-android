
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
    id("de.mobilej.unmock")
    id("org.jetbrains.dokka")
}

android {

    namespace = "com.wultra.android.mtokensdk"

    compileSdk = Constants.Android.compileSdkVersion
    buildToolsVersion = Constants.Android.buildToolsVersion

    defaultConfig {
        minSdk = Constants.Android.minSdkVersion

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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = Constants.Java.sourceCompatibility
        targetCompatibility = Constants.Java.targetCompatibility
    }

    buildFeatures {
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = Constants.Java.kotlinJvmTarget
        suppressWarnings = false
    }
}

// Custom ktlint script
tasks.register<Exec>("ktlint") {
    group = "verification"
    description = "Run ktlint via custom script"
    commandLine("./../scripts/lint.sh", "--no-error")
}

// Run ktlint before build
tasks.named("preBuild").configure {
    dependsOn("ktlint")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    // Bundled
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Constants.BuildScript.kotlinVersion}")
    implementation("androidx.annotation:annotation:1.8.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.wultra.android.powerauth:powerauth-networking:1.5.0")

    // Dependencies
    compileOnly("com.wultra.android.powerauth:powerauth-sdk:1.9.2")
    compileOnly("io.getlime.core:rest-model-base:1.9.0")

    // TestDependencies
    testImplementation("junit:junit:4.13.2")

    // Android tests
    androidTestImplementation("com.wultra.android.powerauth:powerauth-sdk:1.9.2")
    androidTestImplementation("com.wultra.android.powerauth:powerauth-networking:1.5.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(platform("org.jetbrains.kotlin:kotlin-bom:${Constants.BuildScript.kotlinVersion}"))
}

apply("android-release-aar.gradle")
