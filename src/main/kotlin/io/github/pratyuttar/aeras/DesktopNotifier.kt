package io.github.pratyuttar.aeras

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * System notifications through the tray icon API (Windows shows them as toast notifications).
 * A notification is only raised while the app window is not the active window.
 */
object DesktopNotifier {
    @Volatile var windowFocused: Boolean = true
    @Volatile var iconImage: java.awt.Image? = null

    private var trayIcon: TrayIcon? = null
    private var lastClick: (() -> Unit)? = null

    fun isSupported(): Boolean = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    @Synchronized
    private fun ensureTrayIcon(): TrayIcon? {
        trayIcon?.let { return it }
        if (!isSupported()) return null
        return runCatching {
                val image = iconImage ?: BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
                TrayIcon(image, AppInfo.NAME).apply {
                    isImageAutoSize = true
                    addActionListener { lastClick?.invoke() }
                    SystemTray.getSystemTray().add(this)
                }
            }
            .getOrNull()
            ?.also { trayIcon = it }
    }

    fun notify(title: String, text: String, isError: Boolean = false, onClick: (() -> Unit)? = null) {
        if (windowFocused) return
        val icon = ensureTrayIcon() ?: return
        lastClick = onClick
        runCatching {
            icon.displayMessage(
                title,
                text,
                if (isError) TrayIcon.MessageType.ERROR else TrayIcon.MessageType.INFO,
            )
        }
    }

    @Synchronized
    fun dispose() {
        trayIcon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
    }
}
