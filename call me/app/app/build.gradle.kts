plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.vibe.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vibe.callme"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            // the phone reaches the laptop's backend through "adb reverse tcp:4000 tcp:4000"
            buildConfigField("String", "API_BASE", "\"http://127.0.0.1:4000\"")
        }
        release {
            buildConfigField("String", "API_BASE", "\"https://api.vibe.invalid\"") // real server address before launch
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // debug key for now so the release build installs on test phones; real keystore before Play Store
            signingConfig = signingConfigs.getByName("debug")
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
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    // gift animations from the admin panel: animated WEBP/GIF (coil-gif), Lottie JSON, SVGA (MP4 plays with media3)
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("com.airbnb.android:lottie-compose:6.6.0")
    implementation("com.github.yyued:SVGAPlayer-Android:2.6.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // login: Firebase phone OTP + Google (Credential Manager), then our own backend tokens
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // invite links: the Play Store passes the friend's code to the app after install
    implementation("com.android.installreferrer:installreferrer:2.2")
    // coin payments (checkout only; the server makes orders and checks every payment)
    implementation("com.razorpay:checkout:1.6.40")
    // video calls: WebRTC (prebuilt Google libwebrtc for Android); signalling goes over our Socket.IO
    implementation("io.getstream:stream-webrtc-android:1.3.8")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // live connection to the server (online status, profile watch, ringing)
    implementation("io.socket:socket.io-client:2.1.0") { exclude(group = "org.json", module = "json") }
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    // host application: live intro video, live selfie with a face check, live ID photo with the number read on the phone
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.camera:camera-video:1.4.1")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
}
