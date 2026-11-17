plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.agiso.routine"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.agiso.routine"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // 매번 같은 키로 서명해야 새 버전을 덮어 설치할 수 있습니다(기록 유지).
    signingConfigs {
        create("agiso") {
            storeFile = file("agiso.jks")
            storePassword = "agiso1234"
            keyAlias = "agiso"
            keyPassword = "agiso1234"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("agiso")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("agiso")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
