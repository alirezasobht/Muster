import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val versionProperties = rootProject.layout.projectDirectory.file("version.properties")
val appVersionName = versionSetting(providers, versionProperties, "VERSION_NAME").get()
val localProperties = rootProject.layout.projectDirectory.file("local.properties")
val uploadKeystorePath = buildSetting(providers, localProperties, "UPLOAD_KEYSTORE_PATH").get()
val uploadKeystorePassword = buildSetting(
    providers,
    localProperties,
    "UPLOAD_KEYSTORE_PASSWORD"
).get()
val isProd = buildSetting(providers, localProperties, "MUSTER_ENV").get() == "prod"

android {
    namespace = "app.muster"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.muster"
        applicationIdSuffix = if (isProd) ".prod" else ".dev"
        manifestPlaceholders["appLabel"] = if (isProd) "Muster" else "Muster Dev"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = versionCode(appVersionName)
        versionName = appVersionName
    }
    signingConfigs {
        if (uploadKeystorePath.isNotEmpty()) {
            create("release") {
                storeFile = file(uploadKeystorePath)
                storePassword = uploadKeystorePassword
                keyAlias = "upload"
                keyPassword = uploadKeystorePassword
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
