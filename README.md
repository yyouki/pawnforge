# PawnForge

PawnForge is an Android-first Pawn editor for SA-MP and open.mp gamemode development. Its interface intentionally follows the classic Pawno/Qawno desktop layout.

## Brand identity

![PawnForge logo](branding/pawnforge-logo.svg)

Android launcher and title bar use the matching vector logo from `app/src/main/res/drawable/ic_pawnforge_logo.xml`.

## Current milestone: Android IDE shell

Implemented:
- Classic blue title bar, File/Edit/Search/Build/Options/Help menu, and toolbar.
- Pawn source editor with monospaced text, line-number gutter, and basic syntax coloring.
- Open and save source files through Android's Storage Access Framework.
- Compiler and Output tabs with honest engine status.
- Project, Settings, and About navigation panels.
- Persistent workspace-folder selection and configurable include directories.
- Pawn `-i` include-path builder with default `pawno/include`, `qawno/include`, `include`, `includes`, `gamemode`, and workspace-root search folders.
- GitHub Actions workflow that builds and uploads a debug APK.

Not implemented yet:
- The Pawn compiler executable/library for Android.
- Real compilation and .amx output.
- SAF workspace staging that preserves nested project folders in app-private storage.
- Pawn compiler argument builder with repeated `-i<directory>` entries and output-file handling.
- Full multi-file project explorer and editor undo/redo.

The Compile action intentionally reports that the compiler engine is missing. It does not fabricate a successful build or create a fake .amx file.

## Include handling

PawnForge adds the workspace root and common `pawno/include`, `qawno/include`, `include`, `includes`, and `gamemode` directories to the Pawn compiler include search list. See [docs/INCLUDE_PATHS.md](docs/INCLUDE_PATHS.md) for quoted and angle-bracket include examples. Workspace selection, staging, and argument construction are implemented; real compilation and compile diagnostics remain pending until the native Pawn compiler engine is integrated.

## Build

Requirements:
- JDK 17
- Android SDK Platform 35
- Gradle 8.9

Run the following command:

    gradle --no-daemon :app:assembleDebug

APK output:

    app/build/outputs/apk/debug/app-debug.apk

Every push to main triggers the Android APK workflow. Download the artifact named pawnforge-debug-apk from the successful workflow run.

## Project structure

    app/
      src/main/
        java/com/youki/pawnforge/MainActivity.kt
        res/values/
        AndroidManifest.xml
    .github/workflows/android.yml

See [Pawn include handling](docs/INCLUDE_PATHS.md) for examples using `#include "gamemode/player.inc"` and `#include <gamemode/player.inc>`.
