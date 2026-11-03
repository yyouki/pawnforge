# Public Release Checklist

PawnForge is distributed as a directly installable APK through free GitHub Releases. Google Play publication is optional. Complete these checks before calling a version stable.

## Direct APK distribution

- [ ] CI publishes the versioned `app-release.apk` to [GitHub Releases](https://github.com/yyouki/pawnforge/releases/latest).
- [ ] Verify the APK installs on Android 7.0 and Android 16.
- [ ] Verify a release update installs over the previous version using the same signing certificate.
- [ ] Configure a permanent signing keystore as described in [APK_DISTRIBUTION.md](APK_DISTRIBUTION.md). The temporary CI debug key is for initial sideload testing only.

## Build and signing

- [ ] Configure the upload-key secrets in GitHub Actions:
  - `PAWNFORGE_UPLOAD_KEYSTORE_BASE64`: base64-encoded private publisher keystore (JKS or PKCS12).
  - `PAWNFORGE_RELEASE_STORE_PASSWORD`: keystore password.
  - `PAWNFORGE_RELEASE_KEY_ALIAS`: upload-key alias.
  - `PAWNFORGE_RELEASE_KEY_PASSWORD`: upload-key password.
- [ ] Confirm `app-release.apk` is signed with the persistent publisher certificate.
- [ ] Store an offline backup of the upload key and its recovery information. Never commit keystores or passwords.
- [ ] Confirm the application ID, version code, version name, and signing certificate are the intended permanent release identity.
- [ ] Keep the previous production signing and version metadata for future updates.

## Compatibility and quality

- [ ] CI passes JVM tests and Android instrumentation tests on Android 7.0 (API 24) and Android 16 (API 36).
- [ ] Test installation, editing, keyboard behavior, status/navigation bars, display cutouts, file selection, saving, and compilation on physical devices from multiple manufacturers.
- [ ] Test both gesture and three-button navigation, portrait and landscape, small screens, and large font/display settings.
- [ ] Validate representative SA-MP and open.mp projects, third-party includes, compiler errors, and generated AMX files.
- [ ] Review crash logs, accessibility, app startup, and performance on low-memory devices.

## Optional Google Play publication

- [ ] Host [PRIVACY_POLICY.md](PRIVACY_POLICY.md) at a stable public HTTPS URL and add that URL to the store listing.
- [ ] Complete Play Console Data safety, content rating, target audience, app access, and app category declarations accurately.
- [ ] Prepare a 512×512 Play icon, feature graphic, phone/tablet screenshots, short description, and full description.
- [ ] Verify current Google Play target API, package, signing, and policy requirements before submission.
- [ ] Upload the signed AAB to an internal testing track first; review the Play pre-launch report before production rollout.
- [ ] Publish release notes and keep a rollback/incident plan.

A successful GitHub Release does not replace physical-device testing, a persistent-signing-key backup, privacy review, or optional Play Console requirements.
