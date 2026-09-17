# GitHub APK releases

The owner will configure a permanent signing key. No private key is generated or stored by this repository. A production APK is not available until that configuration is supplied; the preview APK is debug-signed and is only for evaluation.

## Signing identity across stores

Android updates require a compatible app signing certificate. If Play generates its own app signing key, an APK locally signed with the **upload key** cannot update an installation signed by Play, or vice versa. For one installation to move seamlessly between GitHub and Play, use the same app signing identity: supply your own app signing key when enrolling in Play App Signing, or distribute a suitably signed APK obtained from Play. Keep the distinction between an upload key and an app signing key clear. See [Android's signing guide](https://developer.android.com/studio/publish/app-signing).

Back up the permanent private key and its passwords securely outside Git. The four `CANDLR_*` variables in [PLAY_RELEASE.md](PLAY_RELEASE.md) must be set in the local build process. Never send the key or passwords in chat. Record the public certificate SHA-256 independently from Android Studio or keytool.

## Build and verify

With those variables configured, run:

```powershell
.\scripts\prepare_release.ps1 -AndroidSdk 'C:\path\to\Android\Sdk' -CertificateSha256 'YOUR_PUBLIC_CERTIFICATE_SHA256'
```

This requires signing configuration, runs unit tests and lint, builds the optimized release APK and AAB, verifies the APK signature against the expected owner certificate, rejects the Android debug certificate, audits offline permissions and ELF alignment, checks ZIP alignment, and generates hashes. It does not perform physical-device testing or grant Play approval.

Install `artifacts/release/Candlr-1.0.0.apk` on a phone and complete the remaining checks in [VALIDATION.md](VALIDATION.md). A debug-signed evaluation installation must be backed up and uninstalled before installing with the permanent certificate; restore its manual backup afterward.

## Publish

Use the repository's draft release for `v1.0.0`. Update its target commit to the validated commit, attach `Candlr-1.0.0.apk`, `SHA256SUMS.txt`, and `package-audit.json`, then publish only after signing and device checks pass. Keep `mapping.txt` with the release records; the AAB is for Play Console. Do not attach the debug preview as the production APK. Subsequent versions need a higher Android version code and a new GitHub tag.

Release notes source: [releases/v1.0.0.md](releases/v1.0.0.md).
