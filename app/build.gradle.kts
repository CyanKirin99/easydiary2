// 文件位置: app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.easydiary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.easydiary"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // 启用 Compose
    buildFeatures {
        compose = true
    }

    // 明确指定 KCE 版本
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // 统一 Java 编译环境为 Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {

    // 统一版本号
    val roomVersion = "2.6.1"
    val navVersion = "2.7.5"

    val composeBomVersion = "2024.05.00"
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))

    // Compose 核心依赖
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // 核心 M3 库
    implementation("androidx.compose.material3:material3")

    // 图标库
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // --- 其他依赖 (保持不变) ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // --- Room 数据库 ---
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // --- Compose Navigation ---
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // --- DataStore (L14/L19) ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // (*** 修复: 删除了失败的日历库 ***)
    // implementation("com.github.kizitonwose:Calendar:2.5.1")

    // (测试依赖...)
}