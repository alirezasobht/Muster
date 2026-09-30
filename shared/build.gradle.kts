import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Supabase URL and publishable key are injected at build time rather than
// hardcoded. The task lives in buildSrc; see CONTEXT.md -> "Decisions already
// closed" for why it is a generated file and not a plugin.

val localProperties = rootProject.layout.projectDirectory.file("local.properties")
val versionProperties = rootProject.layout.projectDirectory.file("version.properties")

val generateSupabaseConfig by tasks.registering(GenerateSupabaseConfig::class) {
    url.set(buildSetting(providers, localProperties, "SUPABASE_URL"))
    publishableKey.set(
        buildSetting(providers, localProperties, "SUPABASE_PUBLISHABLE_KEY")
    )
    webAppUrl.set(buildSetting(providers, localProperties, "WEB_APP_URL"))
    outputDir.set(layout.buildDirectory.dir("generated/supabase"))
}

val generateAppInfo by tasks.registering(GenerateAppInfo::class) {
    versionName.set(versionSetting(providers, versionProperties, "VERSION_NAME"))
    outputDir.set(layout.buildDirectory.dir("generated/appInfo"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "app.muster.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain {
            kotlin.srcDir(generateSupabaseConfig)
            kotlin.srcDir(generateAppInfo)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.supabase.postgrest)
                implementation(libs.supabase.auth)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.viewmodel.navigation)
                implementation(libs.navigation.compose)
                implementation(libs.reorderable)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotlin.testJunit)
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
            }
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.kotlin.testJunit)
                implementation(libs.junit)
                implementation(libs.androidx.testExt.junit)
                implementation(libs.compose.uiTest)
                implementation(libs.compose.uiTestJUnit4)
                implementation(libs.compose.uiTestManifest)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
            // kotlinx-datetime has no IANA tz database of its own on JS;
            // without this, TimeZone.of("Australia/Sydney") compiles but
            // throws IllegalTimeZoneException at runtime.
            implementation(npm("@js-joda/timezone", "2.25.2"))
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            // Same gap as jsMain, wasmJs has its own copy of the module.
            implementation(npm("@js-joda/timezone", "2.25.2"))
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
