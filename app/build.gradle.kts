import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.lab.assistant"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.lab.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1-downloads"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.mediapipe.genai)
    implementation(libs.workmanager)
}
