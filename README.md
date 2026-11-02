# PawnForge

PawnForge is an Android-first Pawn editor and compiler frontend inspired by the classic Pawno/Qawno interface for SA-MP and open.mp gamemode development.

## Project status

The first milestone is the Android IDE shell: classic toolbar/menu layout, editable Pawn source, syntax coloring, file open/save, project navigation, and compiler/output panels.

The Pawn compiler engine is **not integrated yet**. The UI must not report successful compilation until a real compiler binary has been built for Android and wired into the app.

## Build

Open this repository in Android Studio, or run `gradle :app:assembleDebug` with Gradle 8.9 and JDK 17. A GitHub Actions workflow builds the debug APK on pushes to `main`.
