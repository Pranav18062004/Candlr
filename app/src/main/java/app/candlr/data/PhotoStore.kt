package app.candlr.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoStore(private val context: Context) {
    val directory = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(name: String): File {
        require(name.matches(Regex("[a-f0-9]{64}\\.jpg"))) { "Invalid photo name." }
        return File(directory, name)
    }

    suspend fun import(uri: Uri): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth in 1..30000 && bounds.outHeight in 1..30000) {
                "Choose a supported photo."
            }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            val original =
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(
                        it,
                        null,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    )
                } ?: error("Could not open the photo.")
            val orientation =
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                } ?: 1
            val matrix =
                Matrix().apply {
                    when (orientation) {
                        2 -> setScale(-1f, 1f)
                        3 -> setRotate(180f)
                        4 -> setScale(1f, -1f)
                        5 -> {
                            setRotate(90f)
                            postScale(-1f, 1f)
                        }
                        6 -> setRotate(90f)
                        7 -> {
                            setRotate(270f)
                            postScale(-1f, 1f)
                        }
                        8 -> setRotate(270f)
                    }
                }
            val upright =
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            val scale = minOf(1f, 512f / maxOf(upright.width, upright.height))
            val resized =
                Bitmap.createScaledBitmap(
                    upright,
                    (upright.width * scale).toInt().coerceAtLeast(1),
                    (upright.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            val bytes =
                ByteArrayOutputStream().use { out ->
                    resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.toByteArray()
                }
            val name = hash(bytes) + ".jpg"
            file(name).writeBytes(bytes)
            if (resized !== upright) resized.recycle()
            if (upright !== original) upright.recycle()
            original.recycle()
            name
        }

    companion object {
        fun hash(bytes: ByteArray) =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
            }
    }
}
