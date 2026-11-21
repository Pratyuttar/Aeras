package io.github.pratyuttar.aeras

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

/** Small wrappers around the operating-system integration the app needs. */
object Platform {
    fun openFile(file: File) {
        if (!file.exists()) throw java.io.FileNotFoundException(file.path)
        Desktop.getDesktop().open(file)
    }

    fun openUrl(url: String) {
        runCatching { Desktop.getDesktop().browse(URI(url)) }
            .onFailure {
                runCatching {
                    when {
                        AppDirs.isWindows ->
                            ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start()
                        AppDirs.isMac -> ProcessBuilder("open", url).start()
                        else -> ProcessBuilder("xdg-open", url).start()
                    }
                }
            }
    }

    /** Opens the file manager with the given file selected (or its folder when that is not possible). */
    fun revealInFileManager(file: File) {
        runCatching {
            when {
                AppDirs.isWindows && file.exists() ->
                    ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
                AppDirs.isMac && file.exists() -> ProcessBuilder("open", "-R", file.absolutePath).start()
                else -> {
                    val dir = if (file.isDirectory) file else file.parentFile
                    if (dir != null && dir.exists()) Desktop.getDesktop().open(dir)
                }
            }
        }
    }

    fun openFolder(dir: File) {
        runCatching {
            dir.mkdirs()
            Desktop.getDesktop().open(dir)
        }
    }

    fun copyToClipboard(text: String) {
        runCatching {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        }
    }

    fun readClipboardText(): String? =
        runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
            }
            .getOrNull()
}
