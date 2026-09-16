# Candlr execution plan

## Confirmed brief

- Native Android birthday app for a book reader.
- Warm paper and old-book feeling inspired by Zen Browser's release notes in both themes.
- Subtle, integrated animation; small package and low battery use.
- Fully offline application: no account, backend, analytics, ads, or INTERNET permission.
- Manual backup and restore with user-selected destinations through Android's file picker.
- Android 8.0 / API 26 minimum, with particular attention to Android 15 and later.
- Birthdays only for the first release. Approximate reminder timing is acceptable.
- Defer contact import, Birday import, widgets, and other event types.

## Working defaults

- Candlr remains the working name.
- Deliver an installable APK initially; public store publishing is outside the current scope.
- English initially, with strings externalized for future localization.
- Standard portable backup initially; password protection is deferred because it was not requested in the clarification.
- Reference performance phone is not yet specified. Record exact hardware for all eventual measurements.

## Design specification

Reference: https://zen-browser.app/release-notes/
Inspected stylesheet: https://zen-browser.app/_astro/Layout.CS--xBkb.css

Zen's observed base tokens:

| Role | Light | Dark |
| --- | --- | --- |
| Background | #F2F0E3 | #1F1F1F |
| Primary text | #2E2E2E | #D1CFC0 |
| Subtle overlay | black at approximately 5% | white at approximately 10% |

Candlr proposed additions, to validate in screen designs:

| Role | Light | Dark |
| --- | --- | --- |
| Raised surface | #E8E5D8 | #292925 |
| Secondary text | #666457 | #AAA899 |
| Bookmark accent | #62694B | #BEC69F |

- Use a readable literary serif for major headings and the next person's name; use system sans-serif for controls, dates, and small text. Evaluate a locally bundled font with minimal weights and retain its license.
- Make the home screen feel like a personal birthday book: open spacing, month headings, compact entries, and a small bookmark motif for favorites.
- Keep primary content left aligned. Use soft corners on interactive surfaces without putting every row in a card.
- Convey paper through color and typography, without full-screen texture assets or distressed lettering.
- Preserve the chosen palette instead of replacing it with wallpaper-derived dynamic colors.
- Provide light, dark, and system selection.
- Verify contrast, TalkBack, 48 dp touch targets, large text, and small-screen layouts.

Home layout concept:

```text
Candlr                              Search

Next birthday
Maya Rao
Tomorrow / Turning 28

September
  Portrait  Arjun Shah                24
  Portrait  Sara Thomas               29

October
  Portrait  ...                      ...

                         Add birthday
Upcoming          Calendar        Settings
```

Multiple birthdays today receive equal visibility. Empty states directly invite adding a birthday or restoring a backup.

## Motion specification

- Control feedback: approximately 120-160 ms.
- Sheet and navigation transitions: approximately 180-260 ms.
- Gentle opacity, small displacement, and color transitions with no pronounced overshoot.
- Favorite bookmark changes smoothly; no bouncing or particles.
- Calendar changes use short directional movement without simulated page curls.
- Save persists immediately; feedback never introduces a waiting period.
- No confetti, looping flame, shimmer, automatic staggered list entrances, blur, or video.
- Stop offscreen animation, respect system animation settings, and include reduced motion.
- Prototype any shared-element transition early and retain it only if performance is sound.

## First-release screens and behavior

1. Upcoming: chronological groups, search, favorites filter, and add action.
2. Add/edit: name, day/month, optional year, optional photo, notes, favorite, reminder override.
3. Person: next occurrence, days remaining, turning age when known, edit and delete with undo.
4. Calendar: month navigation, marked dates, and selected-day birthday list.
5. Settings: theme, reduced motion, reminder defaults, February 29 policy, backup and restore.
6. Backup/restore: export, validated preview, merge/replace choice, conflicts, progress and result.

## Technical foundation

- Kotlin, Jetpack Compose, custom Material 3 theme, ViewModels and StateFlow.
- Room for birthday records; DataStore for preferences; private files for resized photos and thumbnails.
- Coroutines for I/O. AlarmManager for reminders, with no persistent service or polling loop.
- Versioned JSON serialization and a portable archive for backup.
- Explicit dependency wiring; one app module initially and a benchmark module as needed.
- Pin compatible stable toolchain/dependency versions at implementation time.
- Separate pure date calculations, storage, reminder scheduling, and archive parsing from UI.

Records use stable IDs, month/day, nullable birth year, name, photo reference, notes, favorite, reminder overrides, and modification metadata. Compute age and next occurrence. Do not store birthdays as UTC instants.

## Reminders and date correctness

- Default around 9 AM device-local time, on the birthday, with optional 1/3/7-day advance reminders.
- Per-person overrides; group simultaneous notifications and deep-link to relevant content.
- Contextual notification permission request on applicable Android versions.
- Reschedule after changes, restore, reboot, update, and clock/time-zone changes.
- Prevent duplicate delivery with occurrence-specific identifiers.
- Validate leap years, February 29 policy, year rollover, unknown years, DST, time-zone changes, and midnight rollover while open.
- Describe approximate timing honestly; test idle mode and manufacturer restrictions. Force-stop remains a platform limitation.

## Offline and backup guarantees

- Audit merged release manifest and dependencies for networking capabilities.
- Disable automatic Android cloud backup and configure version-appropriate data extraction exclusions.
- The app never uploads. A user-selected external file provider may perform its own cloud transfer.
- Export a consistent archive with manifest, schema version, records, portable settings, and photos. Exclude device permission state and delivery history.
- Confirm success only after output closes successfully. Handle picker cancellation and storage errors.
- Validate records, supported versions, image references, entry paths, and archive size limits before changing live data.
- Restore preview offers merge or replace. Stable IDs prevent repeated-import duplicates; names alone never establish identity.
- Stage immutable photos before transactional data changes; use recovery metadata and a temporary snapshot to recover interrupted replacement. Avoid deleting existing photo files until success.
- Rebuild reminders after commit. Recover scheduling on next launch if interrupted.
- Clean temporary data and reject malformed or unsupported backups without altering current records.

## Execution sequence and gates

1. Design: light/dark screen compositions, reusable tokens, motion prototype, and critique against the brief.
   Gate: readable, cohesive designs for populated, empty, error, and large-text states.
2. Foundation: Gradle project, navigation, theme, Room, preferences, and pure birthday logic.
   Gate: builds and passes date/persistence tests on the supported API range.
3. Core UI: add/edit/delete, photos, search, favorites, details, and calendar.
   Gate: complete flows work in airplane mode and survive recreation.
4. Reminders: scheduling, permissions, grouped notifications, and rescheduling.
   Gate: device tests cover closed app, reboot, time changes, idle, denial, and duplicate prevention.
5. Backup: export, validation, preview, merge, replace, and recovery.
   Gate: round trips preserve records/photos/preferences and failure scenarios preserve current data.
6. Polish: restrained motion, accessibility, dark theme, keyboard/insets, and visual review.
   Gate: screenshots and interactions reviewed on small and modern phones with enlarged text.
7. Optimization: R8/resource shrinking, baseline profiles, release benchmarks, and dependency audit.
   Gate: measured results against declared reference hardware; simplify effects that miss budgets.
8. Delivery: APK, build instructions, backup-format notes, test results, and performance report.
   Gate: clean install and upgrade regression pass; report any unverified physical-device scenarios explicitly.

## Provisional performance budgets

- Release APK goal: approximately 15 MB or less, excluding user data.
- Usable cold start goal: within 1 second on a declared reference phone.
- Janky-frame goal: under 1% in defined scrolling and navigation journeys.
- Search goal: within 100 ms for 5,000 records on that phone.
- No continuous background execution; no retained-screen growth in repeated navigation.
- Backup/restore remains responsive with progress, bounded memory, and I/O off the main thread.

These are targets, not measured claims. Test release builds on API 26 and modern Android, with physical-device performance and reminder checks where hardware is available.

## Implementation notes

The first implementation uses a singleton Room preferences table instead of DataStore. Birthday records and portable settings therefore commit together during restore without a second storage system or a cross-store journal. Platform serif typography avoids bundled or downloadable fonts. The automatic EmojiCompat font initializer is disabled.

The preview includes the core screens, local reminders, photo selection, backup preview/merge/replace/recovery, and explicit accessibility labeling. Release optimization uses R8, resource shrinking, and dependency-provided baseline profiles. Custom journey profiles and physical-device performance measurements remain to be completed on a selected reference phone.
