package com.junkfood.seal

import android.content.Context
import android.content.pm.PackageInfo
import android.net.ConnectivityManager
import android.net.Uri
import io.github.pratyuttar.aeras.AppDirs
import io.github.pratyuttar.aeras.AppInfo
import io.github.pratyuttar.aeras.DesktopContext
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.DownloaderV2Impl
import com.junkfood.seal.ui.page.download.HomePageViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.settings.directory.Directory
import com.junkfood.seal.ui.page.settings.network.CookiesViewModel
import com.junkfood.seal.ui.page.videolist.VideoListViewModel
import com.junkfood.seal.util.AUDIO_DIRECTORY
import com.junkfood.seal.util.COMMAND_DIRECTORY
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.FileUtil.getExternalDownloadDirectory
import com.junkfood.seal.util.FileUtil.getExternalPrivateDownloadDirectory
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.VIDEO_DIRECTORY
import com.junkfood.seal.util.YT_DLP_VERSION
import com.junkfood.seal.util.setLanguage
import com.tencent.mmkv.MMKV
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Desktop counterpart of Seal's Android Application class: process-wide state and start-up. */
class App {
    companion object {
        const val VERSION_NAME = AppInfo.VERSION
        const val VERSION_CODE = 200_000_150L

        var context: Context = DesktopContext
        lateinit var videoDownloadDir: String
        lateinit var audioDownloadDir: String
        lateinit var applicationScope: CoroutineScope
        val connectivityManager = ConnectivityManager()
        val packageInfo = PackageInfo(VERSION_NAME, VERSION_CODE)

        @Volatile private var initialized = false

        /** Must run once before any other part of the app is touched. */
        fun init() {
            if (initialized) return
            initialized = true

            MMKV.initialize(context)
            applicationScope = CoroutineScope(SupervisorJob())
            io.github.pratyuttar.aeras.Tools.adoptBundled()

            startKoin {
                modules(
                    module {
                        single<DownloaderV2> { DownloaderV2Impl(context) }
                        viewModel { DownloadDialogViewModel(downloader = get()) }
                        viewModel { HomePageViewModel() }
                        viewModel { CookiesViewModel() }
                        viewModel { VideoListViewModel() }
                    }
                )
            }

            videoDownloadDir = VIDEO_DIRECTORY.getString(getExternalDownloadDirectory().absolutePath)
            audioDownloadDir =
                AUDIO_DIRECTORY.getString(File(videoDownloadDir, "Audio").absolutePath)
            if (!PreferenceUtil.containsKey(COMMAND_DIRECTORY)) {
                COMMAND_DIRECTORY.updateString(videoDownloadDir)
            }

            setLanguage(PreferenceUtil.getLocaleFromPreference())
        }

        /** Android kept a foreground service alive while downloads ran; nothing to do on desktop. */
        fun startService() {}

        fun stopService() {}

        val privateDownloadDir: String
            get() = getExternalPrivateDownloadDirectory().apply { mkdirs() }.absolutePath

        fun updateDownloadDir(uri: Uri, directoryType: Directory) {
            val path = FileUtil.getRealPath(uri)
            when (directoryType) {
                Directory.AUDIO -> {
                    audioDownloadDir = path
                    PreferenceUtil.encodeString(AUDIO_DIRECTORY, path)
                }

                Directory.VIDEO -> {
                    videoDownloadDir = path
                    PreferenceUtil.encodeString(VIDEO_DIRECTORY, path)
                }

                Directory.CUSTOM_COMMAND -> {}

                Directory.SDCARD -> {}
            }
        }

        fun getVersionReport(): String {
            return StringBuilder()
                .append(
                    "${AppInfo.NAME} $VERSION_NAME for Windows, based on " +
                        "${AppInfo.UPSTREAM_NAME} ${AppInfo.UPSTREAM_VERSION}\n"
                )
                .append(
                    "System: ${System.getProperty("os.name")} ${System.getProperty("os.version")} " +
                        "(${System.getProperty("os.arch")}), Java ${System.getProperty("java.version")}\n"
                )
                .append("Yt-dlp version: ${YT_DLP_VERSION.getString()}\n")
                .toString()
        }

        fun isFDroidBuild(): Boolean = false

        fun isDebugBuild(): Boolean = false

        val dataDirectory: File
            get() = AppDirs.data
    }
}
