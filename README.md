# PawnForge

PawnForge is an Android-first Pawn editor for SA-MP and open.mp gamemode development. Its interface intentionally follows the classic Pawno/Qawno desktop layout.

## Brand identity

![PawnForge logo](branding/pawnforge-logo.svg)

The logo artwork intentionally contains only the pawn symbol and PawnForge wordmark.

Android launcher and title bar use the matching vector logo from `app/src/main/res/drawable/ic_pawnforge_logo.xml`.

## Current milestone: Native Android compiler

- Bundles Pawn Community Compiler 3.10.10 as a pinned source submodule.
- Builds the `pawnc` shared library and JNI bridge with Android NDK.
- Compiles `.pwn` files in-process and captures compiler diagnostics.
- Builds `.amx` output for the selected project, then exports the edited source and generated AMX back through Android's Storage Access Framework.
- Native ABI builds: `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Android minimum API: 24.

The compiler is built from [BagasZkyn/pawn-compiler-android](https://github.com/BagasZkyn/pawn-compiler-android) at pinned revision `c8bebc178045476746b2e1b0aa7108d4c7ed074b`. Its upstream license is preserved in the submodule and app assets.

## Current milestone: Android IDE shell

Implemented:
- Classic blue title bar, File/Edit/Search/Build/Options/Help menu, and toolbar.
- Pawn source editor with monospaced text, line-number gutter, and basic syntax coloring.
- Open and save source files through Android's Storage Access Framework.
- Compiler and Output tabs with native Pawn compiler diagnostics.
- Project, Settings, and About navigation panels.
- Persistent workspace-folder selection and configurable include directories.
- Pawn `-i` include-path builder with default `pawno/include`, `qawno/include`, `include`, `includes`, `gamemode`, and workspace-root search folders.
- GitHub Actions workflow that builds and uploads a debug APK.

Not implemented yet:
- Full physical-device smoke testing and real-world SA-MP/open.mp project validation.
- Full multi-file project explorer and editor undo/redo.

The app reports success only when the native compiler exits successfully and the `.amx` file exists. Compiler warnings and errors are displayed from the compiler output log.

## Include handling

PawnForge adds the workspace root, source directory, bundled compiler includes, and common `pawno/include`, `qawno/include`, `include`, `includes`, and `gamemode` directories to the compiler search list using repeated `-i` arguments. See [docs/INCLUDE_PATHS.md](docs/INCLUDE_PATHS.md) for quoted and angle-bracket include examples.

## Build

Requirements:
- JDK 17
- Android SDK Platform 35
- Gradle 8.9

Run the following command:

    gradle --no-daemon :app:assembleDebug

APK output:

    app/build/outputs/apk/debug/app-debug.apk

Every push to main triggers native builds, unit tests, and APK packaging. Download the artifact named `pawnforge-debug-apk` from the successful workflow run.

## Project structure

    app/
      src/main/
        java/com/youki/pawnforge/MainActivity.kt
        res/values/
        AndroidManifest.xml
    .github/workflows/android.yml

See [Pawn include handling](docs/INCLUDE_PATHS.md) for examples using `#include "gamemode/player.inc"` and `#include <gamemode/player.inc>`.
