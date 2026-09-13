import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 签名配置：本地读 keystore.properties（已在 .gitignore 里），CI 从环境变量注入
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val signingStoreFile = (keystoreProps.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE"))
    ?.let { rootProject.file(it) }
val hasSigning = signingStoreFile?.exists() == true

android {
    namespace = "top.aryun.token"
    compileSdk = 36

    defaultConfig {
        applicationId = "top.aryun.token"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("appVersionName").orNull ?: "1.0"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = signingStoreFile
                storePassword = keystoreProps.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    implementation("androidx.compose.ui:ui:1.9.5")
    implementation("androidx.compose.ui:ui-graphics:1.9.5")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.5")
    implementation("androidx.compose.foundation:foundation:1.9.5")
    implementation("androidx.compose.runtime:runtime:1.9.5")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")

    debugImplementation("androidx.compose.ui:ui-tooling:1.9.5")

    // 单元测试：用真正的 org.json 覆盖 android.jar 里的空实现
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
