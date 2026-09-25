import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))

            implementation(libs.compose.ui)
        }
    }
}

// Static pages served next to the app get their links at build time; Pages
// serves files as-is, so there is nothing to read at runtime.
val localProperties = rootProject.layout.projectDirectory.file("local.properties")
val contactEmail = buildSetting(providers, localProperties, "CONTACT_EMAIL")
val webAppUrl = buildSetting(providers, localProperties, "WEB_APP_URL")

tasks.withType<ProcessResources>().configureEach {
    // Locals, not the script-level vals: capturing those drags the script
    // object into the task, which the configuration cache can't serialize.
    val email = contactEmail
    val url = webAppUrl
    inputs.property("contactEmail", email)
    inputs.property("webAppUrl", url)
    filesMatching("delete-account.html") {
        val emailValue = email.get()
        val urlValue = url.get()
        require(emailValue.isNotBlank() && urlValue.isNotBlank()) {
            "CONTACT_EMAIL and WEB_APP_URL must be set in local.properties or in the environment."
        }
        filter { line -> line.replace("@CONTACT_EMAIL@", emailValue).replace("@WEB_APP_URL@", urlValue) }
    }
}
