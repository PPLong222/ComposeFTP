plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add Ksp
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
//    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "indi.pplong.composelearning"
    compileSdk = 34

    defaultConfig {
        applicationId = "indi.pplong.composelearning"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // System
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // BottomNavigation
//    implementation(libs.androidx.material)

    // Permission
    implementation(libs.accompanist.permissions)

    // FTP
    implementation(libs.commons.net)
    // SFTP
    implementation(libs.sshj) {
        // Fix dependency version issues
        exclude("org.bouncycastle", "bcprov-jdk18on")
    }
    // Add Algo support for X25519 on Android Platform
    implementation(libs.bcprov.jdk15to18)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    // Room
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    // To use Kotlin Symbol Processing (KSP)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)


    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
//    ksp("com.google.dagger:hilt-compiler:2.49")
    implementation(libs.androidx.hilt.navigation.compose)
//    ksp(libs.dagger.hilt.compiler)

    // Splash
    implementation(libs.androidx.core.splashscreen)

    // Coil
    implementation(libs.coil.compose)
}