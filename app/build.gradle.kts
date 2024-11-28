plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    signingConfigs {
        create("Release1") {
            storeFile = file("/Users/pagrawal2/.android/upload-keystore.jks")
            storePassword = "Von@gecobra123"
            keyAlias = "upload"
            keyPassword = "Von@gecobra123"
        }
    }
    namespace = "com.example.myapp2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapp2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("Release1")
    }

    buildTypes {
        release {
            isDebuggable = false
            signingConfig = signingConfigs.getByName("Release1")
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    flavorDimensions += listOf()
    dependenciesInfo {
        includeInApk = rootProject.extra["includeInApk"] as Boolean
        includeInBundle = true
    }
    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    releaseImplementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.client.sdk)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics:22.1.2")
    // Firebase Cloud Messaging Dependency
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    //Others
    val appcompat_version = "1.7.0"
    val core_telecom_version = "1.0.0-alpha03"
    val accompanist_permissions_version = "0.36.0"
    val vonage_jwt_version = "2.0.0"
    val material3_icons_core_version = "1.3.1"
    val material_icons_core_version = "1.7.5"
    val lifecycle_runtime_compose_version = "2.8.7"
    val compose_runtime_version = "1.7.5"
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("androidx.activity:activity-compose")
    // For loading and tinting drawables on older versions of the platform
    implementation("androidx.appcompat:appcompat-resources:$appcompat_version")
    implementation("androidx.core:core-telecom:$core_telecom_version")
    implementation("com.google.accompanist:accompanist-permissions:$accompanist_permissions_version")
    implementation("com.vonage:jwt:$vonage_jwt_version")
    implementation("androidx.compose.material3:material3:$material3_icons_core_version")
    implementation("androidx.compose.material:material-icons-core:$material_icons_core_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_runtime_compose_version")
    implementation("androidx.compose.runtime:runtime:$compose_runtime_version")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

}
