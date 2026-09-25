plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace  = "com.example.callvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.callvault"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 4
        versionName   = "4.0"
    }

    buildTypes {
        debug {
            val t = providers.gradleProperty("TELEGRAM_BOT_TOKEN").orElse("")
            val c = providers.gradleProperty("TELEGRAM_CHAT_ID").orElse("")
            buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${t.get()}\"")
            buildConfigField("String", "TELEGRAM_CHAT_ID",   "\"${c.get()}\"")
        }
        release {
            val t = providers.gradleProperty("TELEGRAM_BOT_TOKEN").orElse("")
            val c = providers.gradleProperty("TELEGRAM_CHAT_ID").orElse("")
            buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${t.get()}\"")
            buildConfigField("String", "TELEGRAM_CHAT_ID",   "\"${c.get()}\"")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    // WorkManager for 5 AM scheduled backup
    implementation("androidx.work:work-runtime:2.9.0")
}
