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
