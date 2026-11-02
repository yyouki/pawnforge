plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.youki.pawnforge"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.youki.pawnforge"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
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

    buildTypes {
        release {
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
