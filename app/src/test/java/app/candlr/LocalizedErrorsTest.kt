package app.candlr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.candlr.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 28], application = android.app.Application::class)
class LocalizedErrorsTest {
    @Test
    fun everyErrorHasAResourceAndUnexpectedDetailsStayPrivate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BookError.entries.forEach { assertTrue(context.getString(it.resource).isNotBlank()) }
        assertEquals(
            BookError.GENERIC,
            IllegalStateException("private path and details").bookError(),
        )
        assertEquals(BookError.FILE_ACCESS, java.io.IOException("private filename").bookError())
        assertEquals(
            BookError.INVALID_NAME,
            runCatching { Birthday(name = "", month = 1, day = 1).validate() }
                .exceptionOrNull()!!
                .bookError(),
        )
    }

    @Test
    fun countsHandleOneAndManyIndependently() {
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        assertEquals("1 birthday", res.getQuantityString(R.plurals.birthday_count, 1, 1))
        assertEquals("2 birthdays", res.getQuantityString(R.plurals.birthday_count, 2, 2))
        assertEquals("1 photo", res.getQuantityString(R.plurals.photo_count, 1, 1))
    }
}
