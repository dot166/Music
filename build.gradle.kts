import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.aconfig)
}

android {
    namespace = "com.android.music"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.android.music"
        minSdk = 19
        targetSdk = 30
        versionCode = 1
        versionName = versionCode.toString()
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main") {
        val src: List<String> = listOf("src")
        java.directories.addAll(src)
        kotlin.directories.addAll(src)
        val resDirs: List<String> = listOf("res")
        res.directories.addAll(resDirs)
        manifest.srcFile("AndroidManifest.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
}

aconfig {
    textProtoRepo = "https://github.com/GrapheneOS/platform_build_release"
    aconfigFiles = mutableListOf("")
}
