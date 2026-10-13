import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.oneshot"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.oneshot"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val appKeystorePropsFile = project.file("keystore.properties")
            val props = Properties()
            val propFile = when {
                appKeystorePropsFile.exists() -> appKeystorePropsFile
                keystorePropertiesFile.exists() -> keystorePropertiesFile
                else -> null
            }
            if (propFile != null) {
                propFile.inputStream().use { props.load(it) }
            }

            val storeFilePath = System.getenv("ONESHOT_KEYSTORE_PATH")
                ?: props.getProperty("storeFile", "oneshot-release.jks")
            val resolvedStoreFile = if (File(storeFilePath).isAbsolute) {
                File(storeFilePath)
            } else {
                project.file(storeFilePath)
            }

            if (resolvedStoreFile.exists()) {
                storeFile = resolvedStoreFile
                storePassword = System.getenv("ONESHOT_KEYSTORE_PASSWORD")
                    ?: props.getProperty("storePassword", "oneshot2026")
                keyAlias = System.getenv("ONESHOT_KEY_ALIAS")
                    ?: props.getProperty("keyAlias", "oneshot_release")
                keyPassword = System.getenv("ONESHOT_KEY_PASSWORD")
                    ?: props.getProperty("keyPassword", "oneshot2026")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            } else {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
