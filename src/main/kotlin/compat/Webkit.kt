package android.webkit

import io.github.pratyuttar.aeras.DesktopContext
import com.junkfood.seal.util.FileUtil.getCookiesFile

/**
 * Seal on Android collects cookies in a WebView. On desktop the cookie store is the app's
 * `cookies.txt`; this class answers the few questions the UI asks about it.
 */
class CookieManager private constructor() {
    fun hasCookies(): Boolean =
        runCatching {
                DesktopContext.getCookiesFile().takeIf { it.exists() }?.useLines { lines ->
                    lines.any { it.isNotBlank() && (!it.startsWith("#") || it.startsWith("#HttpOnly_")) }
                } ?: false
            }
            .getOrDefault(false)

    fun flush() {}

    fun removeAllCookies(callback: ((Boolean) -> Unit)?) {
        val removed = runCatching { DesktopContext.getCookiesFile().delete() }.getOrDefault(false)
        callback?.invoke(removed)
    }

    companion object {
        private val instance = CookieManager()

        fun getInstance(): CookieManager = instance
    }
}
