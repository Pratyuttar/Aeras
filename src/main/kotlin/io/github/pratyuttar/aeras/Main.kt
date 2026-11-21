package io.github.pratyuttar.aeras

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.junkfood.seal.App
import io.github.pratyuttar.aeras.CrashLog
import io.github.pratyuttar.aeras.DesktopNotifier
import io.github.pratyuttar.aeras.FilePickers
import io.github.pratyuttar.aeras.SelfTest
import io.github.pratyuttar.aeras.ToastHost
import io.github.pratyuttar.aeras.ToolsSetupDialog
import io.github.pratyuttar.aeras.WindowPrefs
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.SettingsProvider
import com.junkfood.seal.ui.page.AppEntry
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.theme.SealTheme
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import kotlin.system.exitProcess
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

fun main() {
    CrashLog.install()
    App.init()

    // Diagnostic mode: AERAS_SELF_TEST=<folder> runs a short end-to-end check in the real app
    // window, writes a report into that folder and exits.
    val selfTest = System.getenv("AERAS_SELF_TEST")?.takeIf { it.isNotBlank() }?.let { File(it) }
    if (selfTest != null && !SelfTest.prepare(selfTest)) exitProcess(1)

    application {
        val saved = remember { WindowPrefs.load() }
        val windowState =
            rememberWindowState(
                placement = if (saved.maximized) WindowPlacement.Maximized else WindowPlacement.Floating,
                position =
                    if (saved.x != null && saved.y != null) WindowPosition(saved.x.dp, saved.y.dp)
                    else WindowPosition.PlatformDefault,
                size = DpSize(saved.width.dp, saved.height.dp),
            )

        Window(
            onCloseRequest = {
                WindowPrefs.save(windowState)
                DesktopNotifier.dispose()
                exitApplication()
            },
            state = windowState,
            title = if (selfTest != null) "${AppInfo.NAME} (self-test)" else AppInfo.NAME,
            icon = AppIcon.painter,
        ) {
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(420, 560)
                FilePickers.ownerFrame = window
                // every size as its own image, so Windows picks an exact fit instead of scaling one
                if (AppIcon.images.isNotEmpty()) window.iconImages = AppIcon.images
                DesktopNotifier.iconImage = AppIcon.image(32) ?: window.iconImages.firstOrNull()
                if (selfTest != null) SelfTest.runInWindow(window, selfTest)
                window.addWindowFocusListener(
                    object : WindowFocusListener {
                        override fun windowGainedFocus(e: WindowEvent?) {
                            DesktopNotifier.windowFocused = true
                        }

                        override fun windowLostFocus(e: WindowEvent?) {
                            DesktopNotifier.windowFocused = false
                        }
                    }
                )
            }
            CompositionLocalProvider(LocalAppWindow provides window) { AerasApp() }
        }
    }
}

/** The whole app UI, as placed inside a window. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AerasApp() {
    val storeOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    if (containerSize.width <= 0 || containerSize.height <= 0) return
    val configuration =
        with(density) {
            Configuration(
                screenWidthDp = containerSize.width.toDp().value.toInt(),
                screenHeightDp = containerSize.height.toDp().value.toInt(),
            )
        }

    KoinContext {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides storeOwner,
            LocalConfiguration provides configuration,
        ) {
            val dialogViewModel: DownloadDialogViewModel = koinViewModel()
            val windowSizeClass = calculateWindowSizeClass()
            SettingsProvider(windowWidthSizeClass = windowSizeClass.widthSizeClass) {
                SealTheme(
                    darkTheme = LocalDarkTheme.current.isDarkTheme(),
                    isHighContrastModeEnabled = LocalDarkTheme.current.isHighContrastModeEnabled,
                ) {
                    TitleBarColors()
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppEntry(dialogViewModel = dialogViewModel)
                        ToolsSetupDialog()
                        ToastHost()
                    }
                }
            }
        }
    }
}
