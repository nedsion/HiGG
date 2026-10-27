package com.higg.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Một bộ icon nét 2dp trên lưới 24×24, tô màu bằng tint của Icon(). */
object HiggIcons {
    val Mic = icon(
        "Mic",
        "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z",
        "M19 10v1a7 7 0 0 1-14 0v-1",
        "M12 18v4",
    )
    val MicOff = icon(
        "MicOff",
        "M9 9v3a3 3 0 0 0 5.1 2.1",
        "M15 9.3V5a3 3 0 0 0-5.7-1.3",
        "M19 10v1a7 7 0 0 1-.7 3",
        "M5 10v1a7 7 0 0 0 11.3 5.5",
        "M12 18v4",
        "M3 3l18 18",
    )
    val Layers = icon("Layers", "M12 3l9 5-9 5-9-5 9-5z", "M3 13l9 5 9-5")
    val Battery = icon("Battery", "M4 7h12a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z", "M22 11v2")
    val Power = icon("Power", "M12 2v10", "M18.4 6.6a9 9 0 1 1-12.8 0")
    val Bell = icon("Bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a2 2 0 0 0 3.4 0")
    val Shield = icon("Shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z", "M9 12l2 2 4-4")
    val CheckCircle = icon("CheckCircle", "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z", "M8 12l3 3 5-6")
    val ChevronRight = icon("ChevronRight", "M9 6l6 6-6 6")
    val ArrowBack = icon("ArrowBack", "M19 12H5", "M12 19l-7-7 7-7")
    val Sparkle = icon("Sparkle", "M12 3l1.9 7.1L21 12l-7.1 1.9L12 21l-1.9-7.1L3 12l7.1-1.9L12 3z")
    val Button = icon("SideButton", "M7 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M20 8v4")
    val Quiet = icon("Quiet", "M11 5L6 9H2v6h4l5 4V5z", "M22 9l-6 6", "M16 9l6 6")
    val Hand = icon("Hand", "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M11 18h2")
    val Repeat = icon("Repeat", "M17 2l4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "M7 22l-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3")
    val Trash = icon("Trash", "M3 6h18", "M8 6V4h8v2", "M19 6l-1 14H6L5 6")
    val Heart = icon(
        "Heart",
        "M12 21l-1.5-1.3C5.4 15.1 2 12.1 2 8.4A4.9 4.9 0 0 1 7 3.5c1.9 0 3.7.9 5 2.3 1.3-1.4 3.1-2.3 5-2.3a4.9 4.9 0 0 1 5 4.9c0 3.7-3.4 6.7-8.5 11.3L12 21z",
    )
    val Lock = icon("Lock", "M6 11h12a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", "M8 11V7a4 4 0 0 1 8 0v4")

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            for (path in paths) {
                addPath(
                    pathData = addPathNodes(path),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
