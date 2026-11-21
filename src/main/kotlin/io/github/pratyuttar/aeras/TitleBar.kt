package io.github.pratyuttar.aeras

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.delay

/** The app's main window. Null where the interface is drawn without one (the self-test picture). */
val LocalAppWindow = staticCompositionLocalOf<ComposeWindow?> { null }

/** The one function of the Windows Desktop Window Manager library that is used here. */
internal interface DwmApi : Library {
    fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
}

/**
 * Colours the native Windows title bar like the app.
 *
 * Windows draws the title bar itself and picks light or dark from its own system-wide setting, not
 * from the app, so a dark app would otherwise sit under a white bar. Windows 11 accepts an exact
 * colour for the bar and its text; Windows 10 only a light/dark switch. Does nothing elsewhere.
 */
object TitleBar {
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19 // Windows 10 before version 2004
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_CAPTION_COLOR = 35 // Windows 11
    private const val DWMWA_TEXT_COLOR = 36 // Windows 11

    private val dwm: DwmApi? by lazy {
        if (!AppDirs.isWindows) null
        else runCatching { Native.load("dwmapi", DwmApi::class.java) }.getOrNull()
    }

    /** Outcome of the latest [apply] call, for the self-test report. */
    @Volatile
    var lastResult: String = "not applied"
        private set

    /** True when Windows accepted at least the light/dark switch (or there was nothing to do). */
    @Volatile
    var lastOk: Boolean = false
        private set

    fun apply(window: ComposeWindow, dark: Boolean, background: Color, text: Color) {
        val api = dwm
        if (api == null) {
            lastOk = !AppDirs.isWindows
            lastResult = if (AppDirs.isWindows) "dwmapi could not be loaded" else "skipped (not Windows)"
            return
        }
        runCatching {
                val handle = window.windowHandle
                check(handle != 0L) { "the window has no native handle yet" }
                val hwnd = Pointer(handle)
                fun set(attribute: Int, value: Int): Int =
                    api.DwmSetWindowAttribute(hwnd, attribute, IntByReference(value), Int.SIZE_BYTES)

                var mode = set(DWMWA_USE_IMMERSIVE_DARK_MODE, if (dark) 1 else 0)
                if (mode != 0) mode = set(DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, if (dark) 1 else 0)
                val caption = set(DWMWA_CAPTION_COLOR, colorRef(background))
                val textColor = set(DWMWA_TEXT_COLOR, colorRef(text))
                lastOk = mode == 0 || caption == 0
                lastResult =
                    "dark=$dark bar=#%06X; Windows answered mode=0x%X colour=0x%X text=0x%X (0 = accepted)"
                        .format(background.toArgb() and 0xFFFFFF, mode, caption, textColor)
            }
            .onFailure {
                lastOk = false
                lastResult = "failed: $it"
            }
    }

    /** Windows COLORREF, 0x00BBGGRR. */
    private fun colorRef(color: Color): Int {
        val argb = color.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (b shl 16) or (g shl 8) or r
    }
}

/** Keeps the title bar in step with the theme. Place inside the app theme. */
@Composable
fun TitleBarColors() {
    val window = LocalAppWindow.current ?: return
    val dark = LocalDarkTheme.current.isDarkTheme()
    val background = MaterialTheme.colorScheme.surface
    val text = MaterialTheme.colorScheme.onSurface
    LaunchedEffect(window, dark, background, text) {
        // the native window is created just after the first composition
        var waited = 0
        while (!window.isDisplayable && waited++ < 200) delay(25)
        TitleBar.apply(window, dark, background, text)
    }
}
