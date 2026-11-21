package io.github.pratyuttar.aeras

import java.io.File

/** Where the desktop app keeps its settings, history, helper binaries and temporary files. */
object AppDirs {
    private val os = (System.getProperty("os.name") ?: "").lowercase()
    val isWindows: Boolean = os.contains("win")
    val isMac: Boolean = os.contains("mac")
    val isLinux: Boolean = !isWindows && !isMac

    // AERAS_HOME_DIR and AERAS_DATA_DIR redirect the app's folders (used by the self-test, and
    // usable for a portable setup).
    private val home: String =
        System.getenv("AERAS_HOME_DIR")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("user.home")
            ?: "."

    const val APP_DIR_NAME = AppInfo.NAME

    /** Settings that only make sense on a phone (SD card, mobile data) stay hidden. */
    const val SHOW_ANDROID_ONLY_SETTINGS = false

    val data: File by lazy {
        val override =
            System.getProperty("aeras.data.dir") ?: System.getenv("AERAS_DATA_DIR")
        val dir =
            when {
                !override.isNullOrBlank() -> File(override)
                isWindows -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", APP_DIR_NAME)
                isMac -> File("$home/Library/Application Support", APP_DIR_NAME)
                else -> File(System.getenv("XDG_CONFIG_HOME") ?: "$home/.config", APP_DIR_NAME)
            }
        dir.apply { mkdirs() }
    }

    val files: File by lazy { File(data, "files").apply { mkdirs() } }
    val cache: File by lazy { File(data, "cache").apply { mkdirs() } }

    /** yt-dlp, ffmpeg and ffprobe live here when the app manages them itself. */
    val bin: File by lazy { File(data, "bin").apply { mkdirs() } }

    val userHome: File = File(home)

    val systemDownloads: File
        get() = File(home, "Downloads")
}
