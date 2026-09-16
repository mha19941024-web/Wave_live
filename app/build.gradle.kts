plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace="com.vyro.app"; compileSdk=37
    defaultConfig { applicationId="com.vyro.app"; minSdk=23; targetSdk=37; versionCode=9; versionName="0.9.0"
        buildConfigField("String", "API_BASE_URL", "\"https://worker-jolly-band-100e.mha19941024.workers.dev\"")
    }
    buildTypes { release { isMinifyEnabled=true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") } }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget="17" }
    buildFeatures { compose=true; buildConfig=true }
}

dependencies {
    val bom=platform("androidx.compose:compose-bom:2026.08.00")
    implementation(bom); androidTestImplementation(bom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
}
