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
        versionCode = 2
        versionName = "0.2.0"

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

val stagePawnCompilerIncludes by tasks.registering(Sync::class) {
    from(rootProject.file("third_party/pawn-compiler-android/include"))
    into(layout.buildDirectory.dir("generated/pawnCompilerAssets/pawn-compiler/include"))
}

tasks.named("preBuild").configure {
    dependsOn(stagePawnCompilerIncludes)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
