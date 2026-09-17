package app.candlr.data

import app.candlr.R

enum class BookError(val resource: Int) {
    INVALID_ID(R.string.error_invalid_id),
    INVALID_NAME(R.string.error_invalid_name),
    NOTES_TOO_LONG(R.string.error_notes_too_long),
    INVALID_REMINDER(R.string.error_invalid_reminder),
    INVALID_DATE(R.string.error_invalid_date),
    INVALID_YEAR(R.string.error_invalid_year),
    FUTURE_DATE(R.string.error_future_date),
    INVALID_PHOTO_REFERENCE(R.string.error_invalid_photo_reference),
    INVALID_PREFERENCES(R.string.error_invalid_preferences),
    INVALID_PHOTO_NAME(R.string.error_invalid_photo_name),
    UNSUPPORTED_PHOTO(R.string.error_unsupported_photo),
    OPEN_PHOTO(R.string.error_open_photo),
    UNSUPPORTED_BACKUP_FILE(R.string.error_unsupported_backup_file),
    DUPLICATE_BACKUP_FILES(R.string.error_duplicate_backup_files),
    BACKUP_TOO_LARGE(R.string.error_backup_too_large),
    UNSUPPORTED_BACKUP_VERSION(R.string.error_unsupported_backup_version),
    DUPLICATE_BIRTHDAYS(R.string.error_duplicate_birthdays),
    DAMAGED_PHOTO(R.string.error_damaged_photo),
    MISSING_PHOTO(R.string.error_missing_photo),
    UNREFERENCED_PHOTO(R.string.error_unreferenced_photo),
    PHOTOS_TOO_LARGE(R.string.error_photos_too_large),
    INVALID_JPEG(R.string.error_invalid_jpeg),
    INVALID_PHOTO(R.string.error_invalid_photo),
    OPERATION_FAILED(R.string.error_operation_failed),
    OPEN_DESTINATION(R.string.error_open_destination),
    OPEN_BACKUP(R.string.error_open_backup),
    CHOOSE_BACKUP(R.string.error_choose_backup),
    GENERIC(R.string.generic),
    INVALID_BACKUP(R.string.invalid_backup),
    FILE_ACCESS(R.string.file_access),
}

class BookException(val reason: BookError) : IllegalArgumentException(reason.name)

fun Throwable.bookError(): BookError =
    when (this) {
        is BookException -> reason
        is kotlinx.serialization.SerializationException,
        is java.util.zip.ZipException -> BookError.INVALID_BACKUP
        is java.io.IOException,
        is SecurityException -> BookError.FILE_ACCESS
        else -> BookError.GENERIC
    }
