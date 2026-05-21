import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

// local.properties から HA_TOKEN・HA_URL を読み込む
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

android {
    namespace = "com.example.shoppinglist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shoppinglist"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // BuildConfig フィールドを追加（token・URL自動設定用）
        buildConfigField("String", "HA_TOKEN", "\"${localProperties.getProperty("HA_TOKEN", "")}\"")
        buildConfigField("String", "HA_URL", "\"${localProperties.getProperty("HA_URL", "http://192.168.1.12:8123")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Modules
    implementation(project(":shared"))

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Glance Widget
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Compose
    implementation("androidx.compose.ui:ui:1.2.1")
    implementation("androidx.compose.material3:material3:1.0.0")
    implementation("androidx.compose.foundation:foundation:1.2.1")
    implementation("androidx.activity:activity-compose:1.6.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
