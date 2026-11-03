# Permanent APK signing and one-time migration

PawnForge 0.5.0 is prepared to use one permanent publisher signing key for all future APK releases.

## 1. Keep the signing kit private

The signing kit is provided separately from this Git repository:

- `pawnforge-publisher-release.jks`: private keystore file.
- `pawnforge-github-secrets.txt`: the four GitHub Actions secret values, including the base64 keystore payload.

Never upload either file to the repository, an issue, a pull request, a public cloud folder, or a public chat. Keep an offline backup of the JKS and the password file. Anyone who obtains both can sign an app update as PawnForge.

Expected signing certificate SHA-256:

`FF:08:FC:90:48:1A:B2:73:13:4D:95:8A:41:5C:EB:58:C3:FA:14:0B:88:64:B0:E6:34:D6:48:03:80:59:92:AC`

CI verifies this exact fingerprint before uploading a release APK. If a different signing key is configured, the workflow fails instead of publishing an incompatible update.

## 2. Add four Actions secrets

Open the repository on GitHub:

**Settings → Secrets and variables → Actions → New repository secret**

Create these four repository secrets. Copy each value from the matching line in the private `pawnforge-github-secrets.txt` file. The base64 value is a long single line.

| Secret name | Value |
|---|---|
| `PAWNFORGE_UPLOAD_KEYSTORE_BASE64` | Base64 JKS payload |
| `PAWNFORGE_RELEASE_STORE_PASSWORD` | Keystore password |
| `PAWNFORGE_RELEASE_KEY_ALIAS` | `pawnforge-release` |
| `PAWNFORGE_RELEASE_KEY_PASSWORD` | Private-key password |

Do not paste any of these values into chat or commit them. The repository workflow only injects them into a trusted build on `main`; pull-request builds never receive the signing key.

## 3. Important one-time migration from 0.4.0

The 0.4.0 CI APK was signed with a temporary GitHub runner debug certificate:

`39:A4:2D:AE:6E:30:DC:BF:60:F8:76:F4:71:4F:A6:25:CE:8F:1F:65:3B:E1:15:30:18:97:90:7B:F8:63:AC:6F`

The private key for that temporary certificate was not retained. Android cannot update an installed app when the new APK has a different signing certificate.

- If 0.4.0 is already installed, back up source files and project data to a folder outside PawnForge's private app storage before migrating.
- After the 0.5.0 APK is built with the permanent certificate, uninstall 0.4.0 once and install 0.5.0.
- If 0.4.0 has not been installed, skip the old version and install 0.5.0 directly.
- Starting with 0.5.0, keep the same JKS forever. New versions with higher versionCode and the same certificate can be installed as normal in-place updates without uninstalling.

Uninstalling clears app-private staging and settings. Files saved in a user-selected shared project folder are outside the app's private storage, but verify your project backup before migrating.

## 4. Release process

1. Add the four repository secrets.
2. Merge the release PR to `main`.
3. CI runs JVM tests, builds the release APK, verifies the permanent certificate fingerprint, and runs Android emulator tests.
4. A successful main-branch workflow publishes the APK to [GitHub Releases](https://github.com/yyouki/pawnforge/releases/latest).

Do not rotate or regenerate the keystore after publishing 0.5.0. Losing the private key prevents future in-place updates signed with that identity.
