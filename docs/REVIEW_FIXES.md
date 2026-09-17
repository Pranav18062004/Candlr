# September 2026 review

The reported issues were reproduced in the source. The exception is the claim that Compose ignores Android's animation duration scale: its animation runtime already observes that setting. The app did lack calendar movement, list placement animations, and one consistent reduced-motion policy.

| Finding | Change |
| --- | --- |
| Singular counts in notifications and accessibility | Android plurals for notification offsets, birthdays, photos, reminder offsets, and restore conflicts. Restore counts are pluralized independently. |
| Resume progress indicator and disabled settings | Reminder reconciliation is silent. Small preference and bookmark changes also avoid the blocking progress state. |
| Cold-start theme mismatch | A small local launch-theme mirror supplies the first Compose state. Android 12+ receives the persistent application night mode, including after replace-restore. Android 8–11 skip the system-themed legacy preview and set the app window background before drawing. Room remains authoritative. |
| Lost tab state | A saveable state holder surrounds page content, preserving search, filters, scroll position, month, and selected date across navigation and activity recreation. |
| Empty Calendar dead end | Calendar always offers the Add birthday action. |
| Dropped rapid actions | A FIFO operation channel replaces the busy early return. Bookmark toggles update the current database value atomically by ID. |
| Repeated avatar decoding and soft detail photos | One application-scoped 8 MiB LRU caches full-resolution saved photos. Cached images are available on the first composition; simultaneous misses share a serialized decode. Memory-trim callbacks release the cache. |
| Editor dismissal loses changes | Scrim, drag, back, and Cancel use the same dirty-draft guard and offer Keep editing or Discard changes. Save bypasses the dismissal guard only after persistence succeeds. |
| Calendar ovals and invisible today | Square constrained date surfaces stay circular. Today keeps a separate outline when another day is selected, and its accessibility description identifies Today. |
| Mismatched date controls | Month uses a read-only outlined dropdown field with the same shape and sizing as Day and Year. |
| Ineffective tablet width limit | The content width is constrained before filling the available space and centered in its full-width parent. |
| Missing themed icon | The adaptive launcher icon includes a monochrome layer. |
| Missing motion | Month grids use short directional transitions and birthday rows animate placement, insertion, and removal. Both the in-app setting and live Android animator-disable setting suppress custom motion; Compose applies nonzero system speed factors. |
| Orphaned photos | Serialized cleanup removes unreferenced images and interrupted staging files after photo-affecting operations and at startup. Active draft and delete-undo references are protected. Recovery archives embed their own photos, so replace-restore can reclaim the old disk files immediately. |
| Hardcoded errors | Validation and storage failures carry typed error codes mapped to Android string resources. Unknown, file-access, and malformed-backup failures display localized generic messages rather than raw exception details. |

One draft photo reference is persisted locally so an Android-restored editor can still display its photo after process death. If Android abandons that task, at most that one draft photo remains until the next editor replaces or clears it. This does not accumulate a photo for every abandoned edit. Delete-undo references are transient, and abandoned ones disappear at process restart.

The launch-theme mirror and draft reference are private local preferences, excluded from automatic backup along with the rest of the app. They add no cloud service or network dependency.

See [VALIDATION.md](VALIDATION.md) for executed checks and remaining device testing.

## Follow-up release review

The 1080×2400 screenshots exceeded Play's mandatory maximum 2:1 ratio. They were recaptured from a 1080×1920 emulator, exported as RGB PNG, and visually checked. `store_assets.py` now asserts count, dimensions, ratio, and absence of alpha. Four 9:16 images are the stronger promotional recommendation, not the general minimum: Google's basic listing requirement is two screenshots. [Official requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en-GB).

- Calendar clicks and selected-date semantics now belong to the whole day cell, independently of the circular highlight. Cells have a minimum 48 dp width and grow with font-scaled line height. On narrow displays the grid scrolls horizontally instead of shrinking targets or clipping text/dots.
- Save, Cancel, and confirmed discard hide the sheet before removing its composition. Save still waits for successful persistence; dirty scrim/Back/drag dismissal remains guarded.
- Photo pruning only runs for photo-affecting queued operations and startup, not bookmark toggles, preference changes, exports, or resume rescheduling.
- The notification channel is created at application startup and idempotently before schedule/delivery, including when reminders or notification permission are disabled.
- Settings shows the existing restore-recovery explanation.
- Photo selection uses AndroidX `PickVisualMedia` with its system fallback for older devices. This is a platform improvement; `GetContent` was not a Play rejection condition.
- `hasFragileUserData` offers Android's optional keep-data uninstall behavior on Android 10+. The privacy text explains that keeping data prevents complete erasure. This flag is also not a Play submission requirement. The two intentional `commit()` calls remain unchanged.
- The previous GitHub CI run failed before building because `sdkmanager` was absent from PATH. The workflow now uses its explicit Android SDK path.

The review's claim that the accessibility scanner necessarily flags every undersized cell was too definite: [Compose expands small touch regions automatically](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), and scan results depend on actual bounds/device configuration. Explicitly sized, non-overlapping cells now remove that ambiguity.

Signing is still a release dependency. The owner elected to configure their own permanent key. A GitHub draft and certificate-verifying release build script are prepared; no debug APK is relabeled as a production release. See [GITHUB_RELEASE.md](GITHUB_RELEASE.md).
