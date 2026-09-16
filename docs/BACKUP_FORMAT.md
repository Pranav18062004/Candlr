# Candlr backup format, version 1

A `.candlr` file is a ZIP archive, written through Android's Storage Access Framework.

| Entry | Contents |
| --- | --- |
| `manifest.json` | `format: "candlr"`, `version: 1`, creation timestamp |
| `birthdays.json` | Array of birthday records with stable UUID identifiers |
| `settings.json` | Portable appearance, reminder, and leap-day preferences |
| `photos/<sha256>.jpg` | Resized, normalized images referenced by records |

Dates are integer month/day plus nullable birth year. They are not UTC timestamps. Reminder masks use bit 0 for the day, bit 1 for one day before, bit 2 for three days, and bit 3 for seven days. Individual masks accept -1 for global defaults and 0 for off.

Import accepts at most 10,000 records, 64 MiB expanded data, 8 MiB per JSON entry, and 1 MiB per photo. Unsupported paths, duplicate entries, duplicate record IDs, invalid dates/settings, missing photos, and photo hash mismatches are rejected. Imported photos must decode with dimensions no larger than 1024 pixels. Archive entries are never extracted directly to entry-specified filesystem paths.

Merge preserves current preferences and adds missing IDs. If any existing ID differs, the user chooses whether the current or backup versions win. People with the same name and different IDs remain distinct. Replacement imports both records and portable settings.

Before restoring, a consistent snapshot of the current book is written and atomically renamed into app-private, no-backup recovery storage. New photos use immutable content-addressed names and are staged before a single Room transaction changes records/preferences. An interrupted database transaction rolls back. Already staged but unreferenced photos cannot affect live records. Old photo files are not removed during the restore transaction.

Delivery history, Android notification permission, and destination URI grants are not restored. Reminders are rebuilt after commit and again on startup. The existing recovery snapshot can be previewed from Settings and restored through the same confirmation flow.

Archives are unencrypted. The content hash detects accidental photo corruption, not malicious archive authorship. Successful export is reported only after closing the output; a provider error may leave an incomplete external file, which should be deleted or overwritten by the owner.
