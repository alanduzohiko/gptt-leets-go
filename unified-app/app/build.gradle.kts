import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// A stable, preview-only signing identity makes repeated sideload APKs install as
// upgrades instead of being rejected for a different ephemeral CI debug key.
// This public key is for development previews only; never use it for production.
val mashxallPreviewKeystoreFile = rootProject.file("../.github/signing/mashxall-preview.keystore")
val mashxallPreviewKeystoreBase64File = rootProject.file("../.github/signing/mashxall-preview.keystore.b64")
if (!mashxallPreviewKeystoreFile.isFile) {
    require(mashxallPreviewKeystoreBase64File.isFile) {
        "Mashxall preview signing key bundle is missing."
    }
    mashxallPreviewKeystoreFile.parentFile?.mkdirs()
    val encodedKeystore = mashxallPreviewKeystoreBase64File.readText().filterNot { it.isWhitespace() }
    mashxallPreviewKeystoreFile.writeBytes(Base64.getDecoder().decode(encodedKeystore))
}

android {
    namespace = "com.mashxall.unified"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mashxall.unified"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("mashxallPreview") {
            storeFile = mashxallPreviewKeystoreFile
            storePassword = "MashxallPreview-2026!!"
            keyAlias = "mashxall-preview"
            keyPassword = "MashxallPreview-2026!!"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("mashxallPreview")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // These files are accessed through AssetManager.openFd / ParcelFileDescriptor:
        // random-access Mushaf streams and the native PdfRenderer need uncompressed assets.
        noCompress += setOf("bin", "pdf")
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/AL2.0",
                "/META-INF/LGPL2.1",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation("junit:junit:4.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.tukaani:xz:1.10")
    implementation("com.caverock:androidsvg-aar:1.4")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
