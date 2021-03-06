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

// Top-level build file where you can add configuration options common to all sub-projects/modules.

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.DefaultConfig
import org.jetbrains.kotlin.gradle.plugin.KaptExtension

buildscript {
    repositories {
        google()
        jcenter()
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
            url = uri("https://maven.fabric.io/public")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath(config.GradleDependency.KOTLIN)
        classpath(config.GradleDependency.SAFE_ARGS)
        classpath(config.GradleDependency.GOOGLE_SERVICE)
//        classpath "org.jacoco:org.jacoco.core:0.8.4"
//        classpath("io.fabric.tools:gradle:1.31.1")
    }
}

plugins {
    id(config.GradleDependency.DETEKT).version(config.GradleDependency.Version.DETEKT)
    id(config.GradleDependency.GRADLE_VERSION_UPDATER).version(config.GradleDependency.Version.VERSION_UPDATER)
    id(config.GradleDependency.DEPENDENCY_GRAPH).version(config.GradleDependency.Version.DEPENDENCY_GRAPH)
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        // required to find the project's artifacts
        maven { url = uri("https://dl.bintray.com/pokk/maven") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://dl.bintray.com/kodein-framework/Kodein-DI") }
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
                suppressWarnings = false
                freeCompilerArgs = listOf(
                    "-Xuse-experimental=kotlin.Experimental",
                    "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
                    "-Xuse-experimental=kotlin.ExperimentalContracts",
                    "-Xuse-experimental=org.mylibrary.ExperimentalMarker",
                    "-Xallow-result-return-type",
                    "-Xjvm-default=all"
                )
            }
        }
    }
}

val modules = config.CommonModuleDependency.getLibraryModuleSimpleName()
val features = config.CommonModuleDependency.getFeatureModuleSimpleName()

subprojects {
    beforeEvaluate {
        //region Apply plugin
        apply {
            when (name) {
                "ext" -> {
                    plugin("java-library")
                    plugin("kotlin")
                }
                in modules -> {
                    plugin("com.android.library")
                    plugin("kotlin-android")
                }
                in features -> {
                    plugin("com.android.dynamic-feature")
                    plugin("kotlin-android")
                    plugin("kotlin-parcelize")
                    plugin("kotlin-kapt")
                    plugin("androidx.navigation.safeargs.kotlin")
                }
            }
            if (name == "core") {
                plugin("org.jetbrains.kotlin.kapt")
            }
            plugin(config.GradleDependency.DETEKT)
            plugin("project-report") // for generating dependency graph
//        plugin("org.jlleitschuh.gradle.ktlint")
        }
        //endregion
    }

    afterEvaluate {
        //region Common Setting
        if (name !in listOf("ext", "feature")) {
            // BaseExtension is common parent for application, library and test modules
            extensions.configure<BaseExtension> {
                compileSdkVersion(config.AndroidConfiguration.COMPILE_SDK)
                defaultConfig {
                    minSdkVersion(config.AndroidConfiguration.MIN_SDK)
                    targetSdkVersion(config.AndroidConfiguration.TARGET_SDK)
                    vectorDrawables.useSupportLibrary = true
                    testInstrumentationRunner = config.AndroidConfiguration.TEST_INSTRUMENTATION_RUNNER
                    consumerProguardFiles(file("consumer-rules.pro"))
                    if (this@subprojects.name in features) {
                        applyRoomSetting()
                    }
                }
                buildTypes {
                    getByName("release") {
                        if (this@subprojects.name !in features) {
                            isMinifyEnabled = true
                        }
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            file("proguard-rules.pro")
                        )
                    }
                    getByName("debug") {
                        splits.abi.isEnable = false
                        splits.density.isEnable = false
                        aaptOptions.cruncherEnabled = false
                        isTestCoverageEnabled = true
                        // Only use this flag on builds you don't proguard or upload to beta-by-crashlytics.
                        ext.set("alwaysUpdateBuildId", false)
                        isCrunchPngs = false // Enabled by default for RELEASE build type
                    }
                }
                dexOptions {
                    jumboMode = true
                    preDexLibraries = true
                    threadCount = 8
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                lintOptions {
                    isAbortOnError = false
                    isIgnoreWarnings = true
                    isQuiet = true
                }
                testOptions {
                    unitTests {
                        isReturnDefaultValues = true
                        isIncludeAndroidResources = true
                    }
                }
                if (this@subprojects.name !in modules) {
                    buildFeatures.viewBinding = true
                }
            }
        }
        if (name in features + listOf("app", "core")) {
            extensions.configure<KaptExtension> {
                useBuildCache = true
                correctErrorTypes = true
                mapDiagnosticLocations = true
            }
        }
        //endregion
    }
}

fun DefaultConfig.applyRoomSetting() {
    javaCompileOptions {
        annotationProcessorOptions {
            arguments["room.schemaLocation"] = "$projectDir/schemas"
            arguments["room.incremental"] = "true"
            arguments["room.expandProjection"] = "true"
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

dependencyGraphGenerator {
    generators = listOf(com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension.Generator.ALL)
}
