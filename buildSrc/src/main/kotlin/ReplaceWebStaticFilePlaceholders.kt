import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Replaces the placeholders in the web app's static files at build time.
 * Pages serves files as-is, so there is nothing to read at runtime.
 */
fun Project.replaceWebStaticFilePlaceholders() {
    val localProperties = rootProject.layout.projectDirectory.file("local.properties")
    // Providers resolved here, outside the task action: capturing the
    // Project drags it into the task, which the configuration cache can't
    // serialize.
    val email = buildSetting(providers, localProperties, "CONTACT_EMAIL")
    val url = buildSetting(providers, localProperties, "WEB_APP_URL")
    val env = buildSetting(providers, localProperties, "MUSTER_ENV")
    val certs = buildSetting(providers, localProperties, "ANDROID_CERT_SHA256")
    val iosId = buildSetting(providers, localProperties, "IOS_APP_STORE_ID")
    val iosTeam = buildSetting(providers, localProperties, "IOS_TEAM_ID")

    tasks.withType<ProcessResources>().configureEach {
        inputs.property("contactEmail", email)
        inputs.property("webAppUrl", url)
        inputs.property("musterEnv", env)
        inputs.property("androidCertSha256", certs)
        inputs.property("iosAppStoreId", iosId)
        inputs.property("iosTeamId", iosTeam)
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
        // Must match the Android build's applicationIdSuffix and the iOS
        // bundle id, which share app.muster.dev / app.muster.prod.
        filesMatching(
            listOf(
                "open.html",
                ".well-known/assetlinks.json",
                ".well-known/apple-app-site-association"
            )
        ) {
            val isProd = env.get() == "prod"
            val appId = if (isProd) "app.muster.prod" else "app.muster.dev"
            val certList = certs.get().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            // A missing fingerprint, App Store id or Team ID builds fine but
            // /open never reaches the app, so prod refuses to build without them.
            require(!isProd || certList.isNotEmpty()) {
                "ANDROID_CERT_SHA256 must be set for a prod web build."
            }
            val iosIdValue = iosId.get()
            require(!isProd || iosIdValue.isNotEmpty()) {
                "IOS_APP_STORE_ID must be set for a prod web build."
            }
            val iosTeamValue = iosTeam.get()
            require(!isProd || iosTeamValue.isNotEmpty()) {
                "IOS_TEAM_ID must be set for a prod web build."
            }
            val certJson = certList.joinToString(", ") { "\"$it\"" }
            filter { line ->
                line.replace("@ANDROID_APP_ID@", appId)
                    .replace("@ANDROID_CERT_SHA256@", certJson)
                    .replace("@IOS_APP_STORE_ID@", iosIdValue)
                    .replace("@IOS_TEAM_ID@", iosTeamValue)
                    .replace("@IOS_BUNDLE_ID@", appId)
            }
        }
    }
}
