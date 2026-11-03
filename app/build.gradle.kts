plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release APKs use the publisher key when configured, otherwise the temporary CI debug key.
val releaseStoreFile = System.getenv("PAWNFORGE_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("PAWNFORGE_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("PAWNFORGE_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("PAWNFORGE_RELEASE_KEY_PASSWORD")
val releaseSigningReady = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.youki.pawnforge"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.youki.pawnforge"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                targets += listOf("pawnforge_jni")
            }
        }
    }

    sourceSets.getByName("main").assets.srcDir(
        layout.buildDirectory.dir("generated/pawnCompilerAssets")
    )

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // For GitHub sideload builds, fall back to the CI debug key if no permanent publisher key is configured.
            signingConfig = if (releaseSigningReady) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

val stagePawnCompilerResources by tasks.registering(Sync::class) {
    into(layout.buildDirectory.dir("generated/pawnCompilerAssets"))
    from(rootProject.file("third_party/pawn-compiler-android/include")) {
        into("pawn-compiler/include")
    }
    from(rootProject.file("third_party/pawn-compiler-android/license.txt")) {
        into("pawn-compiler")
        rename { "LICENSE.txt" }
    }
}

tasks.named("preBuild").configure {
    dependsOn(stagePawnCompilerResources)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
