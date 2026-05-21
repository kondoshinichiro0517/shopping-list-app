#!/bin/bash
# Android プロジェクト初期化スクリプト

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo "📱 Shopping List App プロジェクト初期化..."

# ディレクトリ構成作成
mkdir -p app/src/main/kotlin/com/example/shoppinglist
mkdir -p app/src/main/res/{layout,values,drawable,xml}
mkdir -p wear/src/main/kotlin/com/example/shoppinglist/wear
mkdir -p wear/src/main/res/{layout,values}
mkdir -p shared/src/main/kotlin/com/example/shoppinglist/shared
mkdir -p gradle/wrapper

# settings.gradle.kts
cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx\\..*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ShoppingList"
include(":app")
include(":wear")
include(":shared")
EOF

# build.gradle.kts (root)
cat > build.gradle.kts << 'EOF'
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    kotlin("android") version "1.9.10" apply false
    kotlin("jvm") version "1.9.10" apply false
}
EOF

# app/build.gradle.kts
cat > app/build.gradle.kts << 'EOF'
plugins {
    id("com.android.application")
    kotlin("android")
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
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Compose
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

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
EOF

# wear/build.gradle.kts
cat > wear/build.gradle.kts << 'EOF'
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.shoppinglist.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shoppinglist.wear"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    // Wear
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear.compose:compose-material3:1.1.2")
    implementation("androidx.wear.compose:compose-foundation:1.1.2")

    // Tiles
    implementation("androidx.wear:wear-tiles:1.3.0")
    implementation("androidx.wear:wear-tiles-material:1.3.0")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
EOF

# shared/build.gradle.kts
cat > shared/build.gradle.kts << 'EOF'
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.example.shoppinglist.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
EOF

# gradle/wrapper/gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# .gitignore
cat > .gitignore << 'EOF'
*.class
*.apk
*.dex
*.so
*.so.sha1
.gradle/
build/
.idea/
*.iml
local.properties
.DS_Store
*.jks
*.keystore
EOF

echo "✓ プロジェクト構成完了"
echo "次のステップ："
echo "  1. Android Studio をインストール（https://developer.android.com/studio）"
echo "  2. Android Studio で '$PROJECT_DIR' を開く"
echo "  3. Gradle Sync を待つ（IDE が自動で同期）"
