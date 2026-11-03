# PawnForge

PawnForge is an Android-first Pawn editor for SA-MP and open.mp gamemode development. Its interface intentionally follows the classic Pawno/Qawno desktop layout.

## Brand identity

![PawnForge logo](branding/pawnforge-logo.svg)

The refreshed identity uses a crafted pawn silhouette, terracotta code brackets, parchment highlights, and a deep-teal field. The Android launcher uses an adaptive icon on Android 8+ and a vector fallback for Android 7. The vector artwork is maintained in branding/pawnforge-logo.svg and branding/pawnforge-app-icon.svg.

Android launcher and title bar use the matching vector logo from `app/src/main/res/drawable/ic_pawnforge_logo.xml`.

## Current milestone: Native Android compiler

- Bundles Pawn Community Compiler 3.10.10 as a pinned source submodule.
- Builds the `pawnc` shared library and JNI bridge with Android NDK.
- Compiles `.pwn` files in-process and captures compiler diagnostics.
- Builds `.amx` output for the selected project, then exports the edited source and generated AMX back through Android's Storage Access Framework.
- Native ABI builds: `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Android minimum API: 24 (Android 7.0 Nougat); target and compile SDK: 36 (Android 16).

The compiler is built from [BagasZkyn/pawn-compiler-android](https://github.com/BagasZkyn/pawn-compiler-android) at pinned revision `c8bebc178045476746b2e1b0aa7108d4c7ed074b`. Its upstream license is preserved in the submodule and packaged under `assets/pawn-compiler/LICENSE.txt`; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Android compatibility and display

- Supports Android 7.0 (API 24) and newer; native libraries are packaged for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Targets Android 16 (API 36) for current platform behavior. This also leaves the app compatible with future Play submission policies, but Play Store publication is optional.
- Keeps the system status bar visible so the clock, Wi-Fi/mobile signal, and battery indicators remain available.
- Applies Android 15/16 edge-to-edge insets so the editor starts below the status bar and remains clear of the navigation area and display cutouts.

## Current milestone: Android IDE shell

Implemented:
- Classic blue title bar, File/Edit/Search/Build/Options/Help menu, and toolbar.
- Pawn source editor with monospaced text, live line numbers, basic syntax coloring, auto-indentation, and a modified-file indicator.
- Bounded Undo/Redo history, clipboard Cut/Copy/Paste, line/selection duplicate and delete, `//` comment toggling, indentation controls, Go To Line, and Ctrl-key shortcuts for external keyboards.
- Find Next/Previous with wraparound, Find & Replace/Replace All with optional case matching, built-in Pawn callback snippets, and a searchable native/signature browser.
- Open and save source files through Android's Storage Access Framework.
- Persistent Compiler and Output tabs; compile results remain visible when switching panels.
- Project, Settings, and About navigation panels.
- Persistent workspace-folder selection and configurable include directories.
- Pawn `-i` include-path builder with default `pawno/include`, `qawno/include`, `include`, `includes`, `gamemode`, and workspace-root search folders.
- GitHub Actions workflow that builds a debug APK and a signed, directly installable release APK.

Not implemented yet:
- Full physical-device smoke testing and real-world SA-MP/open.mp project validation.
- Multi-file tabs, complete project explorer, structured compiler diagnostics with jump-to-line, completion, and richer native documentation.

The app reports success only when the native compiler exits successfully and the `.amx` file exists. Compiler warnings and errors are displayed from the compiler output log.

## Release readiness

Version 0.5.0 builds a debug APK and an installable release APK signed with a permanent publisher key. CI verifies the exact signing-certificate fingerprint and runs instrumentation tests on Android 7.0 (API 24) and Android 16 (API 36) emulators. A successful push to `main` publishes the APK to [GitHub Releases](https://github.com/yyouki/pawnforge/releases/latest), which provides free downloads for this public repository. No Play Store account or paid hosting is required for direct APK distribution.

Trusted main-branch releases require the permanent publisher key and verify its certificate fingerprint. Pull-request artifacts use a temporary debug key and are for testing only. See [the one-time 0.4.0 signing migration and permanent key setup](docs/APK_SIGNING_MIGRATION.md). After migrating to 0.5.0, future versions can update in place without uninstalling as long as the original JKS is kept.

## Editor roadmap

See [the Pawn IDE feature roadmap](docs/FEATURE_ROADMAP.md) for the Pawno/Qawno-inspired features already included and the recommended priorities for multi-file tabs, structured diagnostics, autocomplete, native reference, and mobile editing tools.

## Include handling

PawnForge adds the workspace root, source directory, bundled compiler includes, and common `pawno/include`, `qawno/include`, `include`, `includes`, and `gamemode` directories to the compiler search list using repeated `-i` arguments. See [docs/INCLUDE_PATHS.md](docs/INCLUDE_PATHS.md) for quoted and angle-bracket include examples.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36
- Gradle 8.11.1

Run the following command:

    gradle --no-daemon :app:assembleDebug

APK output:

    app/build/outputs/apk/debug/app-debug.apk

Every push to main builds native libraries, runs JVM and Android emulator tests, verifies the release APK signature, and publishes a versioned APK to GitHub Releases when that version has not already been published. Pull requests build and test artifacts but never publish a release.

## Project structure

    app/
      src/main/
        java/com/youki/pawnforge/MainActivity.kt
        res/values/
        AndroidManifest.xml
    .github/workflows/android.yml

See [Pawn include handling](docs/INCLUDE_PATHS.md) for examples using `#include "gamemode/player.inc"` and `#include <gamemode/player.inc>`.
