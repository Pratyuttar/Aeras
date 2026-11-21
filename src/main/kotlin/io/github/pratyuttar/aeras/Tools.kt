package io.github.pratyuttar.aeras

import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Locates the external programs the app drives (yt-dlp, FFmpeg, optionally aria2c) and downloads
 * yt-dlp and FFmpeg into the app's own folder when they are missing.
 */
object Tools {
    private const val YT_DLP_WINDOWS_URL =
        "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"
    private const val YT_DLP_LINUX_URL =
        "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux"
    private const val YT_DLP_MAC_URL =
        "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos"
    private const val FFMPEG_WINDOWS_URL =
        "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip"

    private val exe = if (AppDirs.isWindows) ".exe" else ""

    private val managedYtDlp: File
        get() = File(AppDirs.bin, "yt-dlp$exe")

    private val managedFfmpeg: File
        get() = File(AppDirs.bin, "ffmpeg$exe")

    private val managedFfprobe: File
        get() = File(AppDirs.bin, "ffprobe$exe")

    /**
     * The packaged Windows app carries yt-dlp and FFmpeg in its resources folder, so it works
     * straight after installation. yt-dlp is copied to the app's data folder on first start
     * because it updates itself in place and the install folder may not be writable.
     */
    private val bundledDir: File? by lazy {
        System.getProperty("compose.application.resources.dir")
            ?.let { File(it) }
            ?.takeIf { it.isDirectory }
    }

    private val bundledYtDlp: File?
        get() = bundledDir?.let { File(it, "yt-dlp$exe") }?.takeIf { it.isFile }

    private val bundledFfmpegDir: File?
        get() = bundledDir?.takeIf { File(it, "ffmpeg$exe").isFile }

    fun adoptBundled() {
        runCatching {
            val bundled = bundledYtDlp ?: return
            if (!managedYtDlp.isFile) {
                AppDirs.bin.mkdirs()
                bundled.copyTo(managedYtDlp, overwrite = true)
                if (!AppDirs.isWindows) managedYtDlp.setExecutable(true)
            }
        }
    }

    sealed interface SetupState {
        data object Idle : SetupState

        data class Downloading(val what: String, val fraction: Float) : SetupState

        data class Failed(val message: String) : SetupState

        data object Done : SetupState
    }

    private val mutableSetupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState = mutableSetupState.asStateFlow()

    private fun findOnPath(name: String): String? {
        val path = System.getenv("PATH") ?: return null
        val names = if (AppDirs.isWindows) listOf("$name.exe", "$name.cmd", "$name.bat") else listOf(name)
        for (dir in path.split(File.pathSeparator)) {
            if (dir.isBlank()) continue
            for (n in names) {
                val f = File(dir, n)
                if (f.isFile && f.canExecute()) return f.absolutePath
            }
        }
        return null
    }

    fun ytDlpPath(): String? =
        when {
            managedYtDlp.isFile -> managedYtDlp.absolutePath
            bundledYtDlp != null -> bundledYtDlp!!.absolutePath
            else -> findOnPath("yt-dlp")
        }

    /** Folder passed to yt-dlp as `--ffmpeg-location`, or null to let yt-dlp search the PATH. */
    fun ffmpegLocation(): String? =
        when {
            managedFfmpeg.isFile -> AppDirs.bin.absolutePath
            else -> bundledFfmpegDir?.absolutePath
        }

    fun hasFfmpeg(): Boolean =
        managedFfmpeg.isFile || bundledFfmpegDir != null || findOnPath("ffmpeg") != null

    fun hasYtDlp(): Boolean = ytDlpPath() != null

    /** True when yt-dlp is the app's own copy, which the app may update. */
    fun isYtDlpManaged(): Boolean = managedYtDlp.isFile

    /** A yt-dlp installed elsewhere on this computer, if any. The app never modifies it. */
    fun systemYtDlpPath(): String? = findOnPath("yt-dlp")

    fun hasAria2c(): Boolean = File(AppDirs.bin, "aria2c$exe").isFile || findOnPath("aria2c") != null

    fun isReady(): Boolean = hasYtDlp() && hasFfmpeg()

    /** True when the app can fetch the missing programs by itself on this operating system. */
    fun canAutoInstall(): Boolean = AppDirs.isWindows || (!hasYtDlp() && hasFfmpeg())

    fun missingDescription(): String =
        listOfNotNull("yt-dlp".takeIf { !hasYtDlp() }, "FFmpeg".takeIf { !hasFfmpeg() })
            .joinToString(" + ")

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private fun download(url: String, target: File, label: String) {
        val tmp = File(target.parentFile, target.name + ".part")
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw java.io.IOException("Empty response for $url")
            val total = body.contentLength()
            var done = 0L
            var lastEmit = 0L
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 120) {
                            lastEmit = now
                            mutableSetupState.value =
                                SetupState.Downloading(
                                    label,
                                    if (total > 0) done.toFloat() / total else -1f,
                                )
                        }
                    }
                }
            }
            if (total > 0 && done != total) throw java.io.IOException("Incomplete download of $label")
        }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun extractFfmpegZip(zip: File) {
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            while (true) {
                val entry = zin.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name.replace('\\', '/')
                if (!name.contains("/bin/")) continue
                val fileName = name.substringAfterLast('/')
                if (fileName.isEmpty() || fileName.startsWith("ffplay")) continue
                // file names only: nothing from the archive can be written outside the bin folder
                File(AppDirs.bin, fileName).outputStream().use { out -> zin.copyTo(out) }
            }
        }
    }

    /**
     * Downloads whatever is missing. With [ownCopies] the app's own copies are fetched even when
     * the programs are installed elsewhere on the computer. Progress and the result are published
     * through [setupState].
     */
    suspend fun install(ownCopies: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                    AppDirs.bin.mkdirs()
                    if (if (ownCopies) !managedYtDlp.isFile else !hasYtDlp()) {
                        val url =
                            when {
                                AppDirs.isWindows -> YT_DLP_WINDOWS_URL
                                AppDirs.isMac -> YT_DLP_MAC_URL
                                else -> YT_DLP_LINUX_URL
                            }
                        mutableSetupState.value = SetupState.Downloading("yt-dlp", 0f)
                        download(url, managedYtDlp, "yt-dlp")
                        if (!AppDirs.isWindows) managedYtDlp.setExecutable(true)
                        YoutubeDL.invalidateVersion()
                    }
                    val needFfmpeg =
                        if (ownCopies) AppDirs.isWindows && !managedFfmpeg.isFile else !hasFfmpeg()
                    if (needFfmpeg) {
                        if (!AppDirs.isWindows)
                            throw IllegalStateException(
                                "FFmpeg was not found. Install it with your package manager."
                            )
                        val zip = File(AppDirs.cache, "ffmpeg.zip")
                        mutableSetupState.value = SetupState.Downloading("FFmpeg", 0f)
                        download(FFMPEG_WINDOWS_URL, zip, "FFmpeg")
                        mutableSetupState.value = SetupState.Downloading("FFmpeg", -1f)
                        extractFfmpegZip(zip)
                        zip.delete()
                        if (!managedFfmpeg.isFile || !managedFfprobe.isFile)
                            throw java.io.IOException("FFmpeg archive did not contain the expected files")
                    }
                    mutableSetupState.value = SetupState.Done
                    true
                }
                .getOrElse {
                    it.printStackTrace()
                    mutableSetupState.value =
                        SetupState.Failed(it.message ?: it.javaClass.simpleName)
                    false
                }
        }

    fun resetSetupState() {
        mutableSetupState.value = SetupState.Idle
    }
}
