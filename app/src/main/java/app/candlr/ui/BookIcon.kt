package app.candlr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

enum class Glyph {
    Book,
    Calendar,
    Settings,
    Plus,
    Search,
    Bookmark,
    Back,
    Next,
    Close,
    Candle,
    Edit,
    Check,
    Download,
    Upload,
}

@Composable
fun BookIcon(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    description: String? = null,
    filled: Boolean = false,
) {
    Canvas(modifier.semantics { if (description != null) contentDescription = description }) {
        val factor = size.minDimension / 24f
        scale(factor, factor, pivot = Offset.Zero) {
            fun line(x: Float, y: Float, xx: Float, yy: Float) =
                drawLine(color, Offset(x, y), Offset(xx, yy), 1.5f, StrokeCap.Round)
            fun path(
                vararg points: Pair<Float, Float>,
                close: Boolean = false,
                fill: Boolean = false,
            ) {
                val p =
                    Path().apply {
                        points.forEachIndexed { i, v ->
                            if (i == 0) moveTo(v.first, v.second) else lineTo(v.first, v.second)
                        }
                        if (close) close()
                    }
                if (fill) drawPath(p, color)
                else drawPath(p, color, style = Stroke(1.5f, cap = StrokeCap.Round))
            }
            when (glyph) {
                Glyph.Book -> {
                    path(
                        3f to 4f,
                        10f to 5f,
                        12f to 7f,
                        14f to 5f,
                        21f to 4f,
                        21f to 19f,
                        14f to 20f,
                        12f to 22f,
                        10f to 20f,
                        3f to 19f,
                        close = true,
                    )
                    line(12f, 7f, 12f, 21f)
                }
                Glyph.Calendar -> {
                    path(4f to 5f, 20f to 5f, 20f to 21f, 4f to 21f, close = true)
                    line(4f, 10f, 20f, 10f)
                    line(8f, 3f, 8f, 7f)
                    line(16f, 3f, 16f, 7f)
                    drawCircle(color, 1f, Offset(9f, 14f))
                    drawCircle(color, 1f, Offset(15f, 17f))
                }
                Glyph.Settings -> {
                    listOf(6f, 12f, 18f).forEachIndexed { i, y ->
                        val x = if (i == 1) 15f else 8f
                        line(3f, y, x - 2.5f, y)
                        line(x + 2.5f, y, 21f, y)
                        drawCircle(color, 2.5f, Offset(x, y), style = Stroke(1.5f))
                    }
                }
                Glyph.Plus -> {
                    line(12f, 5f, 12f, 19f)
                    line(5f, 12f, 19f, 12f)
                }
                Glyph.Search -> {
                    drawCircle(color, 7f, Offset(10f, 10f), style = Stroke(1.5f))
                    line(15f, 15f, 21f, 21f)
                }
                Glyph.Bookmark ->
                    path(
                        6f to 3f,
                        18f to 3f,
                        18f to 22f,
                        12f to 18f,
                        6f to 22f,
                        close = true,
                        fill = filled,
                    )
                Glyph.Back -> path(15f to 5f, 8f to 12f, 15f to 19f)
                Glyph.Next -> path(9f to 5f, 16f to 12f, 9f to 19f)
                Glyph.Close -> {
                    line(6f, 6f, 18f, 18f)
                    line(18f, 6f, 6f, 18f)
                }
                Glyph.Candle -> {
                    val p =
                        Path().apply {
                            moveTo(12f, 1f)
                            cubicTo(4f, 9f, 18f, 10f, 12f, 1f)
                        }
                    drawPath(p, color, style = Stroke(1.3f))
                    path(8f to 11f, 16f to 11f, 16f to 22f, 12f to 19f, 8f to 22f, close = true)
                    line(12f, 8f, 12f, 11f)
                }
                Glyph.Edit -> {
                    path(5f to 16f, 16f to 5f, 20f to 9f, 9f to 20f, 4f to 21f, 5f to 16f)
                    line(13f, 8f, 17f, 12f)
                }
                Glyph.Check -> path(4f to 12f, 10f to 18f, 21f to 6f)
                Glyph.Download -> {
                    path(6f to 11f, 12f to 17f, 18f to 11f)
                    line(12f, 3f, 12f, 17f)
                    path(3f to 17f, 3f to 22f, 21f to 22f, 21f to 17f)
                }
                Glyph.Upload -> {
                    path(6f to 9f, 12f to 3f, 18f to 9f)
                    line(12f, 3f, 12f, 17f)
                    path(3f to 17f, 3f to 22f, 21f to 22f, 21f to 17f)
                }
            }
        }
    }
}
