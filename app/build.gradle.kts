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
        versionCode = 3
        versionName = "2.1.0"

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

    // Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // 核心 KTX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Room 数据库
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // DataStore (用于应用设置)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil (用于异步加载图片)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ExifInterface (用于读取图片旋转信息)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // 测试依赖（已移除，避免网络问题）
}