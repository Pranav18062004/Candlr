package app.candlr

import android.app.UiModeManager
import android.content.Context
import android.os.Build

/** Small synchronous launch mirror; Room remains the source of truth. */
object LaunchTheme {
    private var appliedMode: Int? = null

    fun read(context: Context): String =
        context.getSharedPreferences("launch", Context.MODE_PRIVATE).getString("theme", "system")
            ?: "system"

    @Synchronized
    fun save(context: Context, theme: String) {
        if (read(context) != theme)
            context
                .getSharedPreferences("launch", Context.MODE_PRIVATE)
                .edit()
                .putString("theme", theme)
                .commit()
        if (Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(UiModeManager::class.java)
            val mode =
                when (theme) {
                    "dark" -> UiModeManager.MODE_NIGHT_YES
                    "light" -> UiModeManager.MODE_NIGHT_NO
                    else -> UiModeManager.MODE_NIGHT_AUTO
                }
            if (appliedMode != mode) {
                manager.setApplicationNightMode(mode)
                appliedMode = mode
            }
        }
    }
}
