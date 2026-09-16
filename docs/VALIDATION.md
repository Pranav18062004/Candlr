# Validation record

Date: 2026-09-16. Host: Windows 11, JDK 17. Emulator: Android 16 / API 36, x86_64, 1080 × 2400, density 420, software graphics. The emulator was in airplane mode for the passing UI checks.

## Automated checks

- 13 birthday/date tests: today, year rollover, unknown birth years, leap-day policies, invalid dates, future dates, personal reminder overrides, and advance reminders crossing New Year.
- 9 backup-format tests: Unicode/settings round trip, unsafe archive paths, future versions, truncation, oversized data, duplicate IDs, missing/damaged photos, and same-name people remaining distinct.
- 4 Room restore tests: idempotent merge, explicit conflict policy, full replacement and recovery, and invalid-photo failure preserving the current book.
- 3 reminder delivery tests: selected-time boundary, global disable, and delivery deduplication.
- 2 Android UI tests: adding/saving a birthday, activity recreation, search, theme changes, calendar/settings navigation, accessible add action, and editor opening.
- Android lint passes with no errors. Remaining warnings concern newer dependency versions, plural/localization suggestions, unused copy, and an unnecessary API qualifier. Dependencies are intentionally pinned to a compatible toolchain rather than auto-updated.

The JVM tests use Robolectric API 28 where Android services/storage are required. They are not proof of manufacturer alarm behavior on a physical phone.

## Visual review

Reviewed the real Android UI in paper/evening themes, populated and empty upcoming views, calendar, settings, and birthday editor. The review found and fixed an unlabeled floating add button after navigation and default purple switch surfaces. The test fixtures are fictional and only exist in instrumentation tests; the shipped app starts empty.

Screenshots are produced under `artifacts/screenshots/candlr-qa/` after pulling `/sdcard/Download/candlr-qa` from the test emulator.

## Offline audit

The merged preview manifest requests notification permission, boot-completed delivery, and AndroidX's signature-protected receiver permission. It has no INTERNET, network-state, contacts, or broad-storage permission. Automatic backup is disabled with explicit cloud/device-transfer exclusions. Automatic EmojiCompat font initialization is removed. No cloud, analytics, advertisement, or network client SDK is used by the app.

## Packaging and performance

The optimized preview uses R8, resource shrinking, and dependency-supplied baseline profiles. The delivered APK is 1,414,166 bytes (about 1.4 MB). Its APK v2 signature verifies, and the optimized APK installed and launched successfully on the API 36 emulator with no AndroidRuntime crash logged. The fresh-install empty screen was visually checked. It uses the local debug signing certificate for evaluation; production signing is not configured. The checksum is in `artifacts/BUILD_INFO.txt`.

## Remaining validation

- Physical-phone cold start, frame timing/jank, memory, and battery measurements against the provisional budgets.
- Android 8 and Android 15 device execution; the minSdk is 26 and lint checks API compatibility, but the available emulator is API 36.
- Manufacturer idle/reboot restrictions, manual clock/time-zone changes, and notification permission changes on real phones.
- Full TalkBack traversal, large-font/small-screen visual review, photo provider variations, and external document-provider export/restore failures.
- Process-kill fault injection during restore and custom app journey baseline profiles.

This is a working evaluation build, not a claim that every production-readiness gate in the execution plan has been completed.
