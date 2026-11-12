package com.pankobyte.launcher.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Line icons with a 2 px stroke on a 24 px canvas, drawn here so no icon library is needed. */
object LineIcons {
    private fun icon(name: String, filled: Boolean = false, mirror: Boolean = false, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirror).apply {
            path(
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, true, true, cx + r, cy)
        arcTo(r, r, 0f, true, true, cx - r, cy)
        close()
    }

    private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
        moveTo(l + rad, t)
        lineTo(r - rad, t)
        arcTo(rad, rad, 0f, false, true, r, t + rad)
        lineTo(r, b - rad)
        arcTo(rad, rad, 0f, false, true, r - rad, b)
        lineTo(l + rad, b)
        arcTo(rad, rad, 0f, false, true, l, b - rad)
        lineTo(l, t + rad)
        arcTo(rad, rad, 0f, false, true, l + rad, t)
        close()
    }

    val Back by lazy { icon("back", mirror = true) { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) } }
    val Lock by lazy {
        icon("lock") {
            roundRect(5f, 11f, 19f, 21f, 2f)
            moveTo(8f, 11f); lineTo(8f, 7f)
            arcTo(4f, 4f, 0f, false, true, 16f, 7f)
            lineTo(16f, 11f)
        }
    }
    val Backspace by lazy {
        icon("backspace") {
            moveTo(21f, 5f); lineTo(9f, 5f); lineTo(3f, 12f); lineTo(9f, 19f); lineTo(21f, 19f); close()
            moveTo(17f, 9.5f); lineTo(12f, 14.5f)
            moveTo(12f, 9.5f); lineTo(17f, 14.5f)
        }
    }
    /** Telephone handset, for the Emergency button. */
    val Phone by lazy {
        icon("phone") {
            moveTo(5f, 4f); lineTo(9f, 4f); lineTo(11f, 9f); lineTo(8.5f, 10.5f)
            curveTo(9.6f, 12.8f, 11.2f, 14.4f, 13.5f, 15.5f)
            lineTo(15f, 13f); lineTo(20f, 15f); lineTo(20f, 19f)
            curveTo(20f, 20.1f, 19.1f, 21f, 18f, 21f)
            curveTo(10f, 20.5f, 3.5f, 14f, 3f, 6f)
            curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
            close()
        }
    }
    val Brush by lazy {
        icon("brush") {
            moveTo(4f, 20f); lineTo(8f, 19f); lineTo(19f, 8f); lineTo(16f, 5f); lineTo(5f, 16f); close()
            moveTo(14f, 7f); lineTo(17f, 10f)
        }
    }
    val Book by lazy {
        icon("book") {
            moveTo(4f, 5f); curveTo(6.5f, 4f, 9.5f, 4f, 12f, 6f); curveTo(14.5f, 4f, 17.5f, 4f, 20f, 5f)
            lineTo(20f, 19f); curveTo(17.5f, 18f, 14.5f, 18f, 12f, 20f); curveTo(9.5f, 18f, 6.5f, 18f, 4f, 19f); close()
            moveTo(12f, 6f); lineTo(12f, 20f)
        }
    }
    val Music by lazy {
        icon("music") {
            moveTo(9f, 18f); lineTo(9f, 5f); lineTo(20f, 3f); lineTo(20f, 16f)
            circle(6.5f, 18f, 2.5f)
            circle(17.5f, 16f, 2.5f)
        }
    }
    val Gamepad by lazy {
        icon("gamepad") {
            roundRect(2f, 7f, 22f, 17f, 5f)
            moveTo(6f, 12f); lineTo(10f, 12f)
            moveTo(8f, 10f); lineTo(8f, 14f)
            moveTo(15.5f, 13f); lineTo(15.5f, 13.01f)
            moveTo(18f, 11f); lineTo(18f, 11.01f)
        }
    }
    val Camera by lazy {
        icon("camera") {
            moveTo(3f, 8f); lineTo(7f, 8f); lineTo(9f, 5f); lineTo(15f, 5f); lineTo(17f, 8f); lineTo(21f, 8f)
            lineTo(21f, 19f); lineTo(3f, 19f); close()
            circle(12f, 13f, 3.5f)
        }
    }
    val Shield by lazy {
        icon("shield") {
            moveTo(12f, 3f); lineTo(20f, 6f); lineTo(20f, 11f)
            curveTo(20f, 16f, 16.5f, 19.5f, 12f, 21f)
            curveTo(7.5f, 19.5f, 4f, 16f, 4f, 11f)
            lineTo(4f, 6f); close()
            moveTo(9f, 12f); lineTo(11.2f, 14.2f); lineTo(15f, 10f)
        }
    }
    val Grid by lazy {
        icon("grid") {
            roundRect(4f, 4f, 10.5f, 10.5f, 1.5f)
            roundRect(13.5f, 4f, 20f, 10.5f, 1.5f)
            roundRect(4f, 13.5f, 10.5f, 20f, 1.5f)
            roundRect(13.5f, 13.5f, 20f, 20f, 1.5f)
        }
    }
    val Search by lazy {
        icon("search") {
            circle(11f, 11f, 6.5f)
            moveTo(16f, 16f); lineTo(20.5f, 20.5f)
        }
    }
    val Chevron by lazy { icon("chevron", mirror = true) { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) } }
    val Close by lazy {
        icon("close") {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }
    val Moon by lazy {
        icon("moon") {
            moveTo(20f, 14.5f)
            arcTo(8.5f, 8.5f, 0f, true, true, 9.5f, 4f)
            arcTo(8f, 8f, 0f, false, false, 20f, 14.5f)
            close()
        }
    }
    val Sliders by lazy {
        icon("sliders") {
            moveTo(4f, 7f); lineTo(6.5f, 7f)
            circle(9f, 7f, 2.5f)
            moveTo(11.5f, 7f); lineTo(20f, 7f)
            moveTo(4f, 17f); lineTo(12.5f, 17f)
            circle(15f, 17f, 2.5f)
            moveTo(17.5f, 17f); lineTo(20f, 17f)
        }
    }
    val Eye by lazy {
        icon("eye") {
            moveTo(2f, 12f); curveTo(5f, 6f, 19f, 6f, 22f, 12f); curveTo(19f, 18f, 5f, 18f, 2f, 12f); close()
            circle(12f, 12f, 3f)
        }
    }
    val Check by lazy { icon("check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7.5f) } }
    val Clock by lazy {
        icon("clock") {
            circle(12f, 12f, 9f)
            moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15f, 14f)
        }
    }
    val Home by lazy {
        icon("home") {
            moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f); lineTo(20f, 20f); lineTo(4f, 20f); close()
        }
    }
    val ShieldAlert by lazy {
        icon("shieldAlert") {
            moveTo(12f, 3f); lineTo(20f, 6f); lineTo(20f, 11f)
            curveTo(20f, 16f, 16.5f, 19.5f, 12f, 21f)
            curveTo(7.5f, 19.5f, 4f, 16f, 4f, 11f)
            lineTo(4f, 6f); close()
            moveTo(12f, 8f); lineTo(12f, 12.5f)
            moveTo(12f, 16f); lineTo(12f, 16.01f)
        }
    }
    val Warning by lazy {
        icon("warning") {
            moveTo(12f, 4f); lineTo(21f, 20f); lineTo(3f, 20f); close()
            moveTo(12f, 10f); lineTo(12f, 14f)
            moveTo(12f, 17f); lineTo(12f, 17.01f)
        }
    }
    val BellOff by lazy {
        icon("bellOff") {
            moveTo(6f, 17f); lineTo(6f, 11f)
            curveTo(6f, 9.4f, 6.6f, 8f, 7.6f, 6.9f)
            moveTo(10f, 5.3f)
            curveTo(14f, 4f, 18f, 7f, 18f, 11f)
            lineTo(18f, 14f)
            moveTo(4f, 17f); lineTo(17f, 17f)
            moveTo(10f, 20f); lineTo(14f, 20f)
            moveTo(4f, 4f); lineTo(20f, 20f)
        }
    }
    val Refresh by lazy {
        icon("refresh") {
            moveTo(19f, 12f)
            arcTo(7f, 7f, 0f, true, true, 16.5f, 6.6f)
            moveTo(17f, 3f); lineTo(17f, 7f); lineTo(13f, 7f)
        }
    }
    val Key by lazy {
        icon("key") {
            circle(8f, 16f, 3.5f)
            moveTo(10.5f, 13.5f); lineTo(19f, 5f)
            moveTo(16f, 8f); lineTo(18.5f, 10.5f)
        }
    }
    val Minus by lazy { icon("minus") { moveTo(6f, 12f); lineTo(18f, 12f) } }
    val Plus by lazy {
        icon("plus") {
            moveTo(6f, 12f); lineTo(18f, 12f)
            moveTo(12f, 6f); lineTo(12f, 18f)
        }
    }
    /** Solid triangle on the Start button. */
    val Play by lazy { icon("play", filled = true) { moveTo(9f, 7f); lineTo(17f, 12f); lineTo(9f, 17f); close() } }
}
