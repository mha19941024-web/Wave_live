plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "live.wave"
    compileSdk = 35

    defaultConfig {
        applicationId = "Wave.live"
        minSdk = 23
        targetSdk = 35

        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://worker-jolly-band-100e.mha41024.workers.dev\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion =
            "1.5.15"
    }

    packaging {
        resources {
            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(
        platform(
            "androidx.compose:compose-bom:2025.02.00"
        )
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation(
        "androidx.activity:activity-compose:1.10.1"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.7"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.8.7"
    )

    implementation(
        "androidx.media3:media3-exoplayer:1.5.1"
    )

    implementation(
        "androidx.media3:media3-ui:1.5.1"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1"
    )

    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )
}
