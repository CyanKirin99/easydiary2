// 文件: 项目根目录/build.gradle.kts
// 这是项目的顶层构建文件，用于配置构建工具版本。

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Android Gradle 插件
        classpath("com.android.tools.build:gradle:8.13.0")
        // Kotlin Gradle 插件
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        // KSP (Kotlin Symbol Processing) 插件，用于 Room
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.22-1.0.17")
    }
}

// 顶部插件块 - 在这里不声明任何插件，以避免解析错误。
// 所有插件都在 app/build.gradle.kts 中应用。