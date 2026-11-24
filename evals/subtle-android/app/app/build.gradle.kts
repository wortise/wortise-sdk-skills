plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.wordtiles"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.wordtiles"
        minSdk = 24
        targetSdk = 35
        versionCode = 31
        versionName = "3.2.0"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation("com.wortise:android-sdk:1.8.0")
}
