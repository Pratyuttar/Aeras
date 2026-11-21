package android.widget

import android.content.Context
import io.github.pratyuttar.aeras.DesktopToast

class Toast private constructor(private val text: String, private val duration: Int) {
    fun show() {
        DesktopToast.show(text, if (duration == LENGTH_LONG) 3500L else 2000L)
    }

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        fun makeText(context: Context?, text: CharSequence, duration: Int): Toast =
            Toast(text.toString(), duration)

        fun makeText(context: Context, resId: Int, duration: Int): Toast =
            Toast(context.getString(resId), duration)
    }
}
