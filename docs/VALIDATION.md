# Validation record

Date: 17 September 2026. Host: Windows, JDK 17. Emulator: Android 16 / API 36, x86_64, software graphics, airplane mode. These are release-candidate checks; physical-device and Play approval gates remain below.

## Automated checks

- **55 JVM checks passed.** Includes 13 birthday/date cases, 10 backup-codec cases, and 16 Android storage/resource/reminder cases each executed under Robolectric API 26 and API 28. Covers leap dates, year rollover, validation, bounded archives, restore conflicts/recovery, notification singular/plural wording, full-resolution cache reuse, photo cleanup with draft/undo protection, and oversized export rejection before loading photos.
- **10 distinct Android UI tests passed on API 36.** The follow-up suite passed nine initially; its old circle assertion targeted the newly rectangular touch cell. After separating the highlight assertion from the touch-target assertion, that test passed too. Covers add/save/recreation/search, retained tab state/calendar selection, rapid queued bookmarks, silent resume, dirty-editor scrim/Back/Cancel guarding, draft recreation, content width/centering, and live system/in-app motion settings.
- **Tablet checks passed:** width/centering assertion and screenshot journey at 1600 dp wide. Content remains centered and capped at 700 dp.
- **Follow-up calendar checks passed:** 48 dp touch-target assertions at 320 dp width, plus the screenshot journey and target checks at 360 dp width / 2x text. Visual review caught nonlinear font scaling clipping the old dot; the corrected cells use measured text dimensions, and the final capture shows round dots. Narrow grids scroll horizontally. This is not a full TalkBack or every-screen accessibility certification.
- **Large-text journey passed:** 360 dp-wide phone configuration at 1.5x font scale, with screenshots reviewed. Long editor content scrolls; Month and Day have matching outlines and heights.
- **Lint:** zero errors, zero PluralsCandidate warnings, zero MonochromeLauncherIcon warnings. Remaining 21 warnings concern newer dependencies/toolchain, intentional synchronous local-preference writes, Kotlin convenience suggestions, and the Android-10-only uninstall-data attribute. No blanket suppression or baseline hides the reported problems.
- Whitespace/diff checks passed.

Robolectric API 26 exercises Android 8 service/storage behavior in simulation; it is not phone installation or performance testing. The earlier GitHub Actions run failed before the build because sdkmanager was not on PATH. The workflow now uses its explicit SDK path; see GitHub Actions for the latest hosted result.

## Visual and runtime review

Reviewed Paper and Evening, upcoming, calendar, settings, editor, tablet width, and enlarged text. Calendar selection is circular and Today has an independent outline when another date is selected. Month and Day share field styling. Screenshot people are fictional test fixtures and are not seeded in the shipped app.

The optimized APK installed over the debug-signed evaluation installation and launched successfully. With the phone in light mode, selecting Evening and force-stopping/relaunching preserved the dark launch background and content. Eight sampled launch screenshots contained no paper background. These samples are evidence from this emulator run, not exhaustive frame timing or physical-device performance measurements. The opposite Paper/system-dark combination was also checked.

An early screenshot run captured an emulator System UI ANR overlay. It was cleared and clean captures were taken; early images are not used as release screenshots. No Candlr AndroidRuntime crash was observed in the final optimized launch checks.

Local QA images are in artifacts/screenshots/. Selected clean phone images are in store/screenshots/phone/: four 1080?1920 RGB PNGs, freshly captured and checked against both Play upload limits and the stronger promotional recommendation.

## Offline and package audit

scripts/audit_package.py checks the actual optimized APK and merged manifest. Capabilities are notifications, boot-completed delivery, and AndroidX's app-scoped signature receiver permission. There is no INTERNET, network-state, contacts, broad-storage, or exact-alarm permission. Automatic backup is disabled with explicit cloud/device-transfer exclusions. Automatic EmojiCompat font initialization is removed. No cloud, analytics, advertisement, or network-client SDK is used.

The 64-bit native graphics-path libraries have 16 KiB-aligned ELF load segments. APK zip alignment and APK v2 signature verification passed. Bundletool 1.18.3 accepted the release AAB. These checks validate package structure/alignment, not all behavior on a physical 16 KiB-page-size device.

The optimized evaluation APK is **1,507,766 bytes** (about 1.51 MB), using R8, resource shrinking, and dependency-supplied baseline profiles. It uses the local debug certificate. The production AAB is deliberately **unsigned** because the owner has not created an upload key. The signing preflight correctly rejects the missing-key configuration. Checksums are in artifacts/BUILD_INFO.txt and artifacts/package-audit.json.

## Gates before public production launch

- Owner-created/verified Play personal account, upload key, live public privacy-policy URL, required closed testing, and Google production-access/review approval.
- Physical-phone cold start, frame timing/jank, memory, search at 5,000 records, and battery measurements against the provisional budgets.
- Android 8 and Android 15 device installation coverage beyond the available API 36 emulator and API 26/28 Robolectric checks.
- Manufacturer idle/reboot restrictions, clock/time-zone changes, notification permission/channel changes, and unused-app restrictions on real phones.
- Full TalkBack traversal, broader large-text coverage, additional screen sizes/foldables, real photo/document-provider variations, and low-storage failures.
- Process-kill fault injection during restore and custom app-journey baseline profiles.

The repository and artifacts are prepared for release testing. These remaining gates must not be represented as completed. See [PLAY_RELEASE.md](PLAY_RELEASE.md) for owner steps.

The production-preparation script correctly rejected absent owner signing configuration. The package auditor also rejected the actual preview APK when presented as a production artifact, because it has the Android debug certificate. No permanent signing key was created; the owner chose to configure their own.
