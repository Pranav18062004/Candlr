package app.candlr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.candlr.ui.*

class MainActivity : ComponentActivity() {
    private val model: CandlrViewModel by viewModels()
    private var requestedPerson by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedPerson = intent.getStringExtra("personId")
        setContent {
            val book by model.state.collectAsStateWithLifecycle()
            CandlrTheme(book.preferences) {
                val dark =
                    book.preferences.theme == "dark" ||
                        book.preferences.theme == "system" &&
                            androidx.compose.foundation.isSystemInDarkTheme()
                SideEffect {
                    enableEdgeToEdge(
                        statusBarStyle =
                            if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                            else
                                SystemBarStyle.light(
                                    android.graphics.Color.TRANSPARENT,
                                    android.graphics.Color.TRANSPARENT,
                                ),
                        navigationBarStyle =
                            if (dark) SystemBarStyle.dark(0xFF1F1F1F.toInt())
                            else SystemBarStyle.light(0xFFF2F0E3.toInt(), 0xFFF2F0E3.toInt()),
                    )
                }
                CandlrApp(model, book, requestedPerson) { requestedPerson = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedPerson = intent.getStringExtra("personId")
    }
}
