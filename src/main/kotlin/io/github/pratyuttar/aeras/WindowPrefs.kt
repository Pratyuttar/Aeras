package io.github.pratyuttar.aeras

import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.tencent.mmkv.MMKV

/** Remembers the window size and position between runs. */
object WindowPrefs {
    data class Saved(val width: Int, val height: Int, val x: Int?, val y: Int?, val maximized: Boolean)

    private val kv
        get() = MMKV.mmkvWithID("window")

    fun load(): Saved {
        val w = kv.decodeInt("width", 1080).coerceIn(420, 4000)
        val h = kv.decodeInt("height", 780).coerceIn(560, 3000)
        val hasPos = kv.containsKey("x") && kv.containsKey("y")
        return Saved(
            width = w,
            height = h,
            x = if (hasPos) kv.decodeInt("x", 0) else null,
            y = if (hasPos) kv.decodeInt("y", 0) else null,
            maximized = kv.decodeBool("maximized", false),
        )
    }

    fun save(state: WindowState) {
        runCatching {
            val maximized = state.placement == WindowPlacement.Maximized
            kv.encode("maximized", maximized)
            if (!maximized && state.placement == WindowPlacement.Floating) {
                kv.encode("width", state.size.width.value.toInt())
                kv.encode("height", state.size.height.value.toInt())
                if (state.position.isSpecified) {
                    kv.encode("x", state.position.x.value.toInt())
                    kv.encode("y", state.position.y.value.toInt())
                }
            }
            kv.flush()
            MMKV.defaultMMKV().flush()
        }
    }
}
