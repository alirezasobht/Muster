plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(libs.versions.ktlintCli.get())
}

dependencies {
    compileOnly(libs.ktlint.cli.ruleset.core)

    testImplementation(libs.ktlint.rule.engine)
    testImplementation(libs.junit)
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.7")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
