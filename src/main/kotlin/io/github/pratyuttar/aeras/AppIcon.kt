package io.github.pratyuttar.aeras

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The app icon, pre-rendered at the sizes Windows asks for (title bar, taskbar, Alt+Tab).
 *
 * The drawing is made of thin lines that disappear when a large picture is scaled down, so each
 * size is its own file: the small ones are drawn bolder and simpler than the large ones.
 */
object AppIcon {
    // icon.png is the full drawing; icon/ holds the bolder, simpler renditions for small sizes
    private val FILES =
        listOf(16, 20, 24, 32, 40, 48, 64, 128).map { "aeras/icon/icon_$it.png" } + "aeras/icon.png"

    /** One image per size, smallest first. A file that fails to load is skipped. */
    val images: List<BufferedImage> by lazy {
        FILES.mapNotNull { path ->
            runCatching {
                    val stream = checkNotNull(AppIcon::class.java.classLoader.getResourceAsStream(path))
                    stream.use { ImageIO.read(it) }
                }
                .getOrNull()
        }
    }

    /** The smallest image that is at least [size] pixels wide, or the largest one there is. */
    fun image(size: Int): BufferedImage? =
        images.firstOrNull { it.width >= size } ?: images.lastOrNull()

    /** Draws whichever pre-rendered size fits the area it is asked to fill. */
    val painter: Painter by lazy { IconPainter(images.map { it.toComposeImageBitmap() }) }

    private class IconPainter(private val bitmaps: List<ImageBitmap>) : Painter() {
        override val intrinsicSize: Size = Size(256f, 256f)

        override fun DrawScope.onDraw() {
            val wanted = min(size.width, size.height)
            val bitmap =
                bitmaps.firstOrNull { it.width >= wanted - 0.5f } ?: bitmaps.lastOrNull() ?: return
            drawImage(
                image = bitmap,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
    }
}
