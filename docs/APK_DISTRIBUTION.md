# Direct APK distribution

PawnForge can be distributed without Google Play or a paid hosting service. The public GitHub repository uses GitHub Releases as the download page:

https://github.com/yyouki/pawnforge/releases/latest

When code is merged to `main`, CI builds and tests the app. If the version tag does not already exist, it publishes `app-release.apk` as a GitHub Release asset. Change `versionName` and increment `versionCode` in `app/build.gradle.kts` for each new release.

## Install on Android

1. Open the latest GitHub Release link on the Android device.
2. Download `app-release.apk`.
3. Open the downloaded APK from the browser or file manager.
4. Android may ask to allow that app to install unknown apps. Enable it for the app that opened the APK, then return and install PawnForge.

This is a normal Android APK installation. Google Play is not required.

## APK signing and updates

Android checks that an app update is signed by the same signing certificate as the installed version.

- If the repository has no permanent signing key configured, CI uses its temporary debug key. The APK is installable, but a later CI run may use a different debug key. In that case Android will reject an in-place update; uninstall the old build before installing the new one. Copy important source files to the selected project folder first.
- For consistent updates, create one permanent keystore and keep a secure offline backup. Never commit the keystore or its passwords to the repository.
- Add the following repository Actions secrets under **Settings → Secrets and variables → Actions**:
  - `PAWNFORGE_UPLOAD_KEYSTORE_BASE64`: base64 text of the keystore file.
  - `PAWNFORGE_RELEASE_STORE_PASSWORD`: keystore password.
  - `PAWNFORGE_RELEASE_KEY_ALIAS`: key alias.
  - `PAWNFORGE_RELEASE_KEY_PASSWORD`: key password.

Once configured, main-branch release APKs use that permanent certificate. Do not lose the keystore: only a build signed by the same key can update the installed app without uninstalling it.

### Create a permanent keystore

Run this once on a trusted computer with the JDK installed (Android Studio includes a JDK). Replace the sample alias with a name you choose and enter your own strong passwords when prompted:

```bash
keytool -genkeypair -v -keystore pawnforge-release.jks -alias pawnforge -keyalg RSA -keysize 4096 -validity 10000
```

Encode it for the GitHub secret:

**macOS / Linux**
```bash
base64 -w 0 pawnforge-release.jks
```

**Windows PowerShell**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("pawnforge-release.jks"))
```

Copy the resulting single-line text into `PAWNFORGE_UPLOAD_KEYSTORE_BASE64`. Enter the exact passwords and alias in the other three secrets. Keep the original JKS file in a private backup. Do not paste the keystore, its password, or its base64 content into issues, pull requests, or chat.

## What CI publishes

- `pawnforge-debug-apk`: debug build artifact for testing.
- `pawnforge-release-apk`: installable release APK artifact.
- A GitHub Release with `app-release.apk` is created automatically after a successful push to `main`, if that version has not already been released.

Pull request workflows never receive the publisher signing key and never publish releases.
