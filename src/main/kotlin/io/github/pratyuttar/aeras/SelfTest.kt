package io.github.pratyuttar.aeras

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.YT_DLP_AUTO_UPDATE
import com.junkfood.seal.util.YT_DLP_VERSION
import com.tencent.mmkv.MMKV
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.koin.core.context.GlobalContext

/**
 * A short end-to-end check of the app on the machine it runs on, started with the environment
 * variable AERAS_SELF_TEST=<output folder>. It makes sure yt-dlp and FFmpeg are available, downloads
 * a small public test video through the app's own download queue in the real app window, extracts
 * its audio, and renders a picture of the app's interface (off-screen, never from the screen). Results go to report.txt and window.png in the output folder.
 */
object SelfTest {
    private const val TEST_URL =
        "https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/big_buck_bunny.mp4"

    private val lines = mutableListOf<String>()
    private var failed = false

    private fun log(s: String) {
        println(s)
        lines += s
    }

    private inline fun step(name: String, block: () -> String) {
        val start = System.currentTimeMillis()
        try {
            val detail = block()
            log("PASS  $name: $detail  (${System.currentTimeMillis() - start} ms)")
        } catch (e: Throwable) {
            failed = true
            log("FAIL  $name: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun finish(outDir: File): Int {
        log("RESULT=" + if (failed) "FAIL" else "PASS")
        runCatching { File(outDir, "report.txt").writeText(lines.joinToString("\n") + "\n") }
        return if (failed) 1 else 0
    }

    /** First half, before any window exists. Returns false when the test cannot continue. */
    fun prepare(outDir: File): Boolean {
        outDir.mkdirs()
        File(outDir, "report.txt").delete()
        File(outDir, "window.png").delete()
        // never hang: give up after ten minutes whatever happens
        Thread {
                Thread.sleep(600_000)
                log("FAIL  watchdog: the self-test did not finish in time")
                failed = true
                finish(outDir)
                Runtime.getRuntime().halt(2)
            }
            .apply { isDaemon = true }
            .start()

        log("${AppInfo.NAME} self-test, ${LocalDateTime.now()}")
        log(
            "os=${System.getProperty("os.name")} ${System.getProperty("os.version")} " +
                "${System.getProperty("os.arch")}, java=${System.getProperty("java.version")} " +
                "(${System.getProperty("java.home")})"
        )
        log("data=${AppDirs.data}")
        log("home=${AppDirs.userHome}")
        log("resources=${System.getProperty("compose.application.resources.dir")}")

        Tools.systemYtDlpPath()?.let { path ->
            val version =
                runCatching {
                        val p = ProcessBuilder(path, "--version").redirectErrorStream(true).start()
                        p.outputStream.close()
                        p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
                    }
                    .getOrDefault("?")
            log("system yt-dlp (not modified by the app): $path, version $version")
        }

        step("tools") {
            // AERAS_SELF_TEST_OWN_TOOLS=1 fetches the app's own copies even if the programs are
            // installed elsewhere, which exercises the first-run downloader.
            val ownCopies = System.getenv("AERAS_SELF_TEST_OWN_TOOLS") == "1"
            if (ownCopies || !Tools.isReady()) {
                check(ownCopies || Tools.canAutoInstall()) { "missing ${Tools.missingDescription()}" }
                val ok = runBlocking { Tools.install(ownCopies = ownCopies) }
                check(ok) { "install failed: ${Tools.setupState.value}" }
                Tools.resetSetupState()
            }
            check(Tools.isReady()) { "missing ${Tools.missingDescription()}" }
            "yt-dlp=${Tools.ytDlpPath()}, ffmpeg=${Tools.ffmpegLocation() ?: "on PATH"}"
        }
        step("yt-dlp version") {
            val version = checkNotNull(YoutubeDL.version()) { "no version output" }
            // keep the automatic yt-dlp update out of the test run
            PreferenceUtil.encodeString(YT_DLP_VERSION, version)
            PreferenceUtil.updateValue(YT_DLP_AUTO_UPDATE, false)
            version
        }
        if (failed) finish(outDir)
        return !failed
    }

    /**
     * Second half, started from the app's real main window once it is showing. The download queue
     * reacts to state changes through Compose's snapshot system, which only runs while a window
     * exists, so the downloads are run here exactly as in normal use.
     */
    fun runInWindow(window: ComposeWindow, outDir: File) {
        Thread {
                val code =
                    try {
                        Thread.sleep(3000)
                        step("app window") { "renderer=${window.renderApi}, title=${window.title}" }
                        step("window icon") {
                            val sizes = window.iconImages.map { it.getWidth(null) }
                            check(sizes.size >= 5) { "expected the icon in several sizes, found $sizes" }
                            "sizes $sizes"
                        }
                        step("title bar") {
                            check(TitleBar.lastOk) { TitleBar.lastResult }
                            TitleBar.lastResult
                        }

                        val downloader = GlobalContext.get().get<DownloaderV2>()
                        val preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
                        step("video download") {
                            runTask(
                                downloader,
                                Task(url = TEST_URL, preferences = preferences.copy(extractAudio = false)),
                                expectedExtensions = setOf("mp4", "mkv", "webm"),
                            )
                        }
                        step("audio extraction") {
                            runTask(
                                downloader,
                                Task(url = TEST_URL, preferences = preferences.copy(extractAudio = true)),
                                expectedExtensions = setOf("m4a", "opus", "mp3", "aac"),
                            )
                        }
                        step("download history") {
                            var count = 0
                            for (i in 1..25) {
                                count = runBlocking { DatabaseUtil.getDownloadHistoryFlow().first().size }
                                if (count >= 2) break
                                Thread.sleep(200)
                            }
                            check(count >= 2) { "expected 2 history entries, found $count" }
                            "$count entries"
                        }
                        step("app picture") {
                            // Rendered off-screen from the app's own content. Nothing is read
                            // from the screen, so other windows can never end up in the picture.
                            val bytes = renderAppOffscreen(1080, 780)
                            File(outDir, "window.png").writeBytes(bytes)
                            val image = ImageIO.read(File(outDir, "window.png"))
                            val colors = HashSet<Int>()
                            for (x in 0 until image.width step 7) for (y in 0 until image.height step 7) {
                                colors += image.getRGB(x, y)
                            }
                            check(colors.size > 8) { "picture looks blank (${colors.size} colours)" }
                            "${image.width}x${image.height}, ${colors.size} colours sampled"
                        }
                        finish(outDir)
                    } catch (e: Throwable) {
                        failed = true
                        log("FAIL  self-test: $e")
                        finish(outDir)
                    }
                MMKV.defaultMMKV().flush()
                Runtime.getRuntime().halt(code)
            }
            .apply { isDaemon = true }
            .start()
    }

    /** Draws the whole app UI into an image, without a window and without touching the screen. */
    private fun renderAppOffscreen(width: Int, height: Int): ByteArray {
        var result: ByteArray? = null
        var failure: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                val lifecycleOwner =
                    object : LifecycleOwner {
                        val registry = LifecycleRegistry(this)
                        override val lifecycle: Lifecycle
                            get() = registry
                    }
                lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                val scene =
                    ImageComposeScene(width = width, height = height, density = Density(1f)) {
                        CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                            AerasApp()
                        }
                    }
                try {
                    // a second of frames, so navigation transitions and image loading settle
                    var image = scene.render(0)
                    for (frame in 1..60) {
                        image = scene.render(frame * 16_666_667L)
                    }
                    result = image.encodeToData(EncodedImageFormat.PNG)?.bytes
                } finally {
                    scene.close()
                }
            } catch (e: Throwable) {
                failure = e
            }
        }
        failure?.let { throw it }
        return checkNotNull(result) { "could not encode the picture" }
    }

    private fun runTask(
        downloader: DownloaderV2,
        task: Task,
        expectedExtensions: Set<String>,
    ): String {
        downloader.enqueue(task)
        val deadline = System.currentTimeMillis() + 150_000
        while (System.currentTimeMillis() < deadline) {
            when (val state = downloader.getTaskStateMap()[task]?.downloadState) {
                is Task.DownloadState.Completed -> {
                    val file = File(checkNotNull(state.filePath) { "no output file reported" })
                    check(file.isFile && file.length() > 100_000) { "output missing or too small: $file" }
                    check(file.extension.lowercase() in expectedExtensions) {
                        "unexpected output file: ${file.name}"
                    }
                    return "${file.name}, ${file.length() / 1024} KB"
                }
                is Task.DownloadState.Error -> throw state.throwable
                else -> Thread.sleep(250)
            }
        }
        downloader.cancel(task)
        error("timed out")
    }
}
