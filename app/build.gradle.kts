/*
 * MIT License
 *
 * Copyright (c) 2019 SmashKs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import config.AndroidConfiguration
import config.CommonModuleDependency
import config.Dependencies
import config.LibraryDependency
import org.jetbrains.kotlin.gradle.internal.CacheImplementation

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    defaultConfig {
        applicationId = AndroidConfiguration.ID
        versionCode = 1
        versionName = "1.0"
    }
    packagingOptions {
        exclude("META-INF/atomicfu.kotlin_module")
        exclude("META-INF/kotlinx-coroutines-core.kotlin_module")
    }
    dynamicFeatures = CommonModuleDependency.getDynamicFeatureModules()
}

kapt {
    useBuildCache = true
    correctErrorTypes = true
    mapDiagnosticLocations = true
}

dependencies {
    //    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(project(CommonModuleDependency.LIB_CORE))
    api(LibraryDependency.PLAY_CORE)
    api(LibraryDependency.KNIFER)
    api(LibraryDependency.MMKV)
    api(LibraryDependency.COIL)
    implementation(LibraryDependency.FIREBASE_CORE)
    implementation(LibraryDependency.FIREBASE_ANALYTICS)
    implementation(LibraryDependency.STARTUP)
    (Dependencies.androidxKtxDeps.values +
     Dependencies.androidxDeps.values +
     Dependencies.uiDeps.values).forEach(::api)
    kapt(LibraryDependency.ROOM_ANNOTATION)
    kapt(LibraryDependency.LIFECYCLE_COMPILER)
}

fun com.android.build.gradle.internal.dsl.DefaultConfig.buildConfigField(name: String, value: Set<String>) {
    // Generates String that holds Java String Array code
    val strValue = value.joinToString(prefix = "{", separator = ",", postfix = "}", transform = { "\"$it\"" })
    buildConfigField("String[]", name, strValue)
}

apply(mapOf("plugin" to "com.google.gms.google-services"))
