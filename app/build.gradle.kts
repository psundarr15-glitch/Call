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
        versionCode   = 3
        versionName   = "3.0"
    }

    buildTypes {
        // Bug 5 fix: release type was missing Telegram config entirely
        // Both debug and release now receive TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID
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

<<<<<<< HEAD
=======
    // Fix: Java (1.8) and Kotlin (17) JVM targets were mismatched → both set to 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

>>>>>>> 58b4092 (Update call app)
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
}
