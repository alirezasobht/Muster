plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ktlint)
}

ktlint {
    coloredOutput.set(false)
    version.set(libs.versions.ktlintCli.get())
}

dependencies {
    ktlintRuleset(project(":ktlint-rules"))
}

subprojects {
    if (path != ":ktlint-rules") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            coloredOutput.set(false)
            version.set(rootProject.libs.versions.ktlintCli.get())
            filter {
                exclude { it.file.path.contains("/build/") }
            }
        }
        dependencies {
            add("ktlintRuleset", project(":ktlint-rules"))
        }
    }
}
