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
val musterEnv = buildSetting(providers, localProperties, "MUSTER_ENV")
val androidCertSha256 = buildSetting(providers, localProperties, "ANDROID_CERT_SHA256")

tasks.withType<ProcessResources>().configureEach {
    // Locals, not the script-level vals: capturing those drags the script
    // object into the task, which the configuration cache can't serialize.
    val email = contactEmail
    val url = webAppUrl
    val env = musterEnv
    val certs = androidCertSha256
    inputs.property("contactEmail", email)
    inputs.property("webAppUrl", url)
    inputs.property("musterEnv", env)
    inputs.property("androidCertSha256", certs)
    filesMatching(listOf("delete-account.html", "privacy.html")) {
        val emailValue = email.get()
        val urlValue = url.get()
        require(emailValue.isNotBlank() && urlValue.isNotBlank()) {
            "CONTACT_EMAIL and WEB_APP_URL must be set in local.properties or in the environment."
        }
        filter { line ->
            line.replace("@CONTACT_EMAIL@", emailValue).replace("@WEB_APP_URL@", urlValue)
        }
    }
    // Must match the Android build's applicationIdSuffix.
    filesMatching(listOf("open.html", ".well-known/assetlinks.json")) {
        val isProd = env.get() == "prod"
        val appId = if (isProd) "app.muster.prod" else "app.muster.dev"
        val certList = certs.get().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        // A missing fingerprint builds fine but /open never opens the app,
        // so prod refuses to build without one.
        require(!isProd || certList.isNotEmpty()) {
            "ANDROID_CERT_SHA256 must be set for a prod web build."
        }
        val certJson = certList.joinToString(", ") { "\"$it\"" }
        filter { line ->
            line.replace("@ANDROID_APP_ID@", appId).replace("@ANDROID_CERT_SHA256@", certJson)
        }
    }
}
