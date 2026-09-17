# Candlr: Google Play release

Owner-approved identity: **Candlr**, developed by **Pranav Nambiar**, support **pranavnambiar12345@gmail.com**. Free download; no ads, purchases, subscriptions, accounts, or analytics. Application ID: `app.candlr`. Version 1.0.0, version code 2. English is the currently shipped language; UI and errors use Android resources for future translations.

## What is prepared

- Android App Bundle release build with shrinking, optimization, and no debug signing fallback.
- Optional environment-based upload-key signing and a signing configuration check.
- Privacy policy inside Settings and a matching public HTML document in `docs/privacy/index.html`.
- Local reminder-reliability help, support details, and open-source license text inside Settings.
- Store copy in `store/listing/en-US/`, original icon/feature assets, and real-device-emulator screenshots from fictional test records.
- Automated tests, lint, and an offline manifest/package audit. See `VALIDATION.md` for evidence and limits.

This is preparation for testing and submission, not an assertion that Google has approved the app. No Play account, production signing key, public policy hosting, closed testing, or production publication has been created automatically.

## Create the personal developer account later

Use the personal Google account that should own this app, and complete Google's identity, contact, payment, and device-verification steps. Keep the developer display name and public support details consistent with the listing and privacy policy. Personal account verification may make additional owner information public; review what Play Console shows before completing registration.

New personal accounts currently require at least **12 testers opted in continuously for 14 days** in a closed test before applying for production access. Active testing and honest feedback matter; a completed time window alone does not guarantee production access. Internal testing can precede the closed test.

Sources checked 17 September 2026: [personal-account testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en), [target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en). The current new-app target requirement is API 36; this project targets 36 while supporting API 26+.

## Configure an upload key

Create an **upload key**, preferably with Android Studio's Generate Signed Bundle / APK wizard. Enroll in Play App Signing when creating the Play release; let Google manage the app signing key if that is your chosen setup. Store the upload keystore and passwords outside this repository, with secure backups. Do not reuse a company key or the preview debug certificate.

In a local shell or CI secret store, set all four environment variables:

- `CANDLR_KEYSTORE`: absolute path to the upload keystore.
- `CANDLR_KEYSTORE_PASSWORD`: its password.
- `CANDLR_KEY_ALIAS`: the selected upload-key alias.
- `CANDLR_KEY_PASSWORD`: the key password.

Do not put actual passwords into documentation, shell history, screenshots, or committed files. All four values must be set together. None are printed by the Gradle configuration.

```powershell
.\gradlew.bat :app:verifyUploadSigning :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab`. With no signing environment configured, `bundleRelease` intentionally produces an **unsigned preparation artifact**; it cannot be submitted until signed. `verifyUploadSigning` fails clearly in that case. `assemblePreview` always uses the local debug certificate and must not be uploaded as the production app.

Check the bundle signature using JDK `jarsigner -verify -verbose -certs`. Confirm it names the intended upload certificate and is not the Android debug certificate. Keep the R8 mapping at `app/build/outputs/mapping/release/mapping.txt` with each release. Increase `versionCode` for every subsequent upload. Confirm the application ID before the first upload because it identifies the app permanently.

## Play Console forms

1. Create an app named Candlr, default language English, app rather than game, free, with no ads. Suggested category: Tools. The owner must choose the actual target age groups and countries; do not select children simply because birthday records can describe children.
2. Paste the supplied short and full descriptions. Add the support email, 512×512 icon, 1024×500 feature graphic, and at least two reviewed phone screenshots. See [official asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en).
3. Host `docs/privacy/index.html` on a stable, public HTTPS URL, test it signed out, and enter that URL. No policy site has been published by this task. The in-app policy is already available offline. All apps need a public privacy policy: [Google's User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en).
4. Data safety: the current app code does not collect or share data off the device. It has no network permission or collecting SDK. User-initiated exports go to an Android file provider selected by the user; review the final form wording and its exceptions against [Google's Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en). Do not mark account deletion as an app feature: the app creates no accounts. Users can erase all app data through Android storage settings and delete exported files separately.
5. App access: every feature is available without login, a subscription, or credentials. Give reviewers basic instructions to add a birthday, change theme, and export/restore a backup.
6. Complete the content-rating and target-audience questionnaires accurately. No social sharing, messaging, user-generated public content, gambling, financial, health, or advertising features are implemented. Private user-entered names/notes are stored locally.
7. Upload the signed AAB to internal testing. Review Play's automated checks and pre-launch report, fix failures, then run the required closed test and collect feedback. Apply for production access when eligible.

The declaration notes describe the current implementation and are not an automatically submitted form or a legal certification. Review them again if any SDK, permission, monetization, audience, or data flow changes.

## Release gates still requiring execution outside this workspace

- Owner creates/verifies the Play account, upload key, and public privacy-policy URL.
- Test the optimized build on physical Android 8+ and modern phones, including at least one manufacturer's battery restrictions, reboot, permission denial/revocation, and unused-app restrictions.
- Check real photo and document providers; round-trip birthdays, notes, and photos in airplane mode. Test low-storage errors and process interruption during restore.
- Run TalkBack traversal, large-text review, and phone/tablet/foldable checks. Verify both explicit themes after a cold start with the opposite system theme.
- Measure cold start, memory, scrolling, search at 5,000 records, and battery use on a named physical reference phone. Emulator results are not physical-device performance claims.
- Complete the Play closed test, resolve pre-launch report findings, and obtain production access/review approval.

Keep a manual backup when switching from a debug-signed preview to a differently signed Play install; Android will not upgrade an installation signed with a different certificate.
