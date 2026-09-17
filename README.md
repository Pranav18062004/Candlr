# Candlr

A small, offline birthday book for Android 8.0 and newer. Warm paper and evening themes, quiet motion, and a place for the people you want to remember.

## Features

- Add, edit, search, bookmark, and delete birthdays with immediate undo.
- Optional birth year, local photo, and notes; upcoming list and month calendar.
- Paper, evening, or system appearance and a reduced-motion preference.
- Local approximate reminders at a selected time, with 0/1/3/7-day offsets and personal overrides.
- Explicit February 29 policy for non-leap years.
- Manual `.candlr` backup files containing birthdays, preferences, and photos.
- Restore preview, merge conflict policy, full replacement, and recovery of the previous book.
- No account, network permission, analytics, advertisements, or automatic cloud backup.

The Android file picker lets the owner choose the backup destination. A separately installed cloud file provider can upload the selected file; Candlr itself never connects to it. Backups are not password protected.

## Build

Requirements: JDK 17, Android SDK platform 36, build tools 35.0.0, and accepted Android SDK licenses. The Gradle wrapper downloads Gradle 8.11.1. Dependency downloads are needed for building, not for using the app.

Set `ANDROID_HOME` or put the local SDK path in an ignored `local.properties`:

```properties
sdk.dir=C\:/Users/your-name/AppData/Local/Android/Sdk
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assemblePreview
```

On macOS/Linux use `./gradlew` for the same tasks.

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Optimized preview APK: `app/build/outputs/apk/preview/app-preview.apk`.
- Play bundle: `:app:bundleRelease` produces an unsigned AAB until upload-key environment variables are configured. See [Play release instructions](docs/PLAY_RELEASE.md).

**Preview uses the local Android debug certificate and is for evaluation.** Keep a manual backup before changing signing certificates or uninstalling. No private signing key is committed.

## Implementation

Kotlin 2.1.20, Compose, Room, coroutines, Android AlarmManager, and Kotlin serialization. Dependencies are pinned; the app does not require Play Services. Custom vector icons and the platform serif font avoid an icon pack and downloadable fonts.

One deliberate refinement to the original plan: portable preferences live in a singleton Room table alongside birthdays. This lets restore atomically commit both in one transaction, avoids a cross-store recovery journal, and removes an extra DataStore dependency. Notification delivery history is local and excluded from exported archives.

Source structure:

- `data/`: database, pure calendar rules, photo normalization, bounded backup format and restore.
- `reminders/`: future occurrences, one next scheduled alarm, notification delivery and rescheduling.
- `ui/`: palette, typography, custom icons, upcoming/details, editor, calendar, settings, restore.
- `CandlrViewModel`: serialized I/O, user-visible errors, lifecycle-aware screen state.

Database schema version 1 is committed under `app/schemas/`. Future changes must provide migrations; destructive migration fallback is intentionally absent.

## Reminders

Reminders start disabled until the owner enables them in Settings and grants notification permission where required. Times are approximate: Android idle mode, power-saving rules, and manufacturer restrictions may delay delivery. No exact-alarm permission or continuously running service is used.

Reboot, package update, clock/time-zone changes, app opening, record changes, and restore trigger rescheduling. Notifications use an occurrence-specific delivery record to avoid ordinary repeated delivery; a crash between OS notification submission and the database write cannot be made fully transactional. Fixed notification IDs and `onlyAlertOnce` limit the impact of that narrow window. Force-stopped apps cannot resume reminders until opened. Missed previous-day reminders are not replayed.

## Backup format and recovery

See [docs/BACKUP_FORMAT.md](docs/BACKUP_FORMAT.md). Files are intended for the owner's private storage. The app keeps a single local pre-restore recovery copy. Repeating a restore updates that copy.

## Validation

See [docs/VALIDATION.md](docs/VALIDATION.md) for executed checks and remaining device validation. Performance numbers must identify the device and build type; emulator startup observations are not physical-phone benchmarks.

The September review fixes are recorded in [docs/REVIEW_FIXES.md](docs/REVIEW_FIXES.md). Store copy and original graphics are in `store/`; a public-hosting-ready privacy policy is in `docs/privacy/`. A GitHub Actions workflow runs unit tests, lint, unsigned release builds, and the package audit; it does not publish releases. Run `python scripts/audit_package.py --sdk <Android-SDK-path>` after building to audit offline permissions, SDK levels, release flags, and 64-bit ELF alignment.

## Design

Base colors take inspiration from [Zen Browser's release notes](https://zen-browser.app/release-notes/): paper `#F2F0E3`, ink `#2E2E2E`, evening `#1F1F1F`, and ivory `#D1CFC0`. Olive accents and the candle/bookmark line work are specific to Candlr. No Birday code or assets were copied.

The agreed product plan is in [EXECUTION_PLAN.md](EXECUTION_PLAN.md). Widgets, contact import, Birday import, encrypted backups, and additional event types are deferred.
