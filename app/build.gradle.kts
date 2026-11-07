import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// "owner/repo" on GitHub. CI passes it automatically (-Pmindheed.githubRepo=...).
val githubRepo = (findProperty("mindheed.githubRepo") as String?).orEmpty()
// CI passes the run number so every build is newer than the previous one.
val ciVersionCode = (findProperty("mindheed.versionCode") as String?)?.toIntOrNull()

android {
    namespace = "com.mindheed.app"
    compileSdk = 36

    defaultConfig {
        // Permanent once published on a store: never change it afterwards.
        applicationId = "com.mindheed.app"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode ?: 1
        versionName = "0.2.0"

        val configUrl =
            if (githubRepo.isBlank()) "" else "https://raw.githubusercontent.com/$githubRepo/main/config/remote-config.json"
        val releasesUrl =
            if (githubRepo.isBlank()) "https://github.com" else "https://github.com/$githubRepo/releases/latest"
        buildConfigField("String", "CONFIG_URL", "\"$configUrl\"")
        buildConfigField("String", "RELEASES_URL", "\"$releasesUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // The release key never goes into the repo: CI reads it from GitHub secrets.
    val keystoreFile = System.getenv("MINDHEED_KEYSTORE_FILE")
    signingConfigs {
        if (!keystoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("MINDHEED_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MINDHEED_KEY_ALIAS")
                keyPassword = System.getenv("MINDHEED_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
