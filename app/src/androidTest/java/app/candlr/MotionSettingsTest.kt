package app.candlr

import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.candlr.data.Preferences
import app.candlr.ui.CandlrTheme
import app.candlr.ui.LocalReduceMotion
import org.junit.Rule
import org.junit.Test

class MotionSettingsTest {
    @get:Rule val compose = createComposeRule()

    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    @Test
    fun bothAppAndLiveSystemSettingsDisableMotion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val previous =
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val localPreference = mutableStateOf(false)
        instrumentation.uiAutomation.adoptShellPermissionIdentity(
            "android.permission.WRITE_SECURE_SETTINGS"
        )
        try {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            compose.setContent {
                CandlrTheme(Preferences(reduceMotion = localPreference.value)) {
                    Text(if (LocalReduceMotion.current) "Still" else "Animated")
                }
            }
            compose.onNodeWithText("Animated").assertIsDisplayed()
            compose.runOnIdle { localPreference.value = true }
            compose.onNodeWithText("Still").assertIsDisplayed()
            compose.runOnIdle { localPreference.value = false }
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            compose.waitUntil(5000) {
                compose.onAllNodesWithText("Still").fetchSemanticsNodes().isNotEmpty()
            }
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            compose.waitUntil(5000) {
                compose.onAllNodesWithText("Animated").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, previous)
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
