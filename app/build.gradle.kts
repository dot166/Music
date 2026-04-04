import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val keystorePropertiesFile = rootProject.file("keystore.properties")
val useKeystoreProperties = keystorePropertiesFile.canRead()
val keystoreProperties = Properties()
if (useKeystoreProperties) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.aconfig)
}

android {
    if (useKeystoreProperties) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"]!!)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                enableV4Signing = true
            }
        }
    }
    namespace = "com.android.music"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.android.music"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = versionCode.toString()
    }
    buildTypes {
        named("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            if (useKeystoreProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        named("debug") {
            applicationIdSuffix = ".DEV"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    implementation(libs.j.lib)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
}

//aconfig {
//    aconfigFiles = mutableListOf("")
//    isAOSP = false
//}