package io.github.pratyuttar.aeras

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.junkfood.seal.R
import java.io.File

/** The single application context of the desktop app. */
object DesktopContext : Context() {
    override val applicationContext: Context
        get() = this

    override val packageName: String = AppInfo.ID

    override val filesDir: File
        get() = AppDirs.files

    override val cacheDir: File
        get() = AppDirs.cache

    override val dataDir: File
        get() = AppDirs.data

    /**
     * Android intents are how Seal opens and shares things. On desktop: "view" opens the file or
     * URL with the default program, "send" copies shared text to the clipboard or shows a shared
     * file in the file manager.
     */
    override fun startActivity(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val data = intent.data ?: throw IllegalArgumentException("Nothing to open")
                if (data.isWebUrl) Platform.openUrl(data.toString())
                else Platform.openFile(data.toFile() ?: throw IllegalArgumentException("Nothing to open"))
            }

            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val stream = (intent.extras[Intent.EXTRA_STREAM] as? Uri) ?: intent.data
                if (text != null) {
                    Platform.copyToClipboard(text)
                    DesktopToast.show(getString(R.string.link_copied))
                } else if (stream != null) {
                    stream.toFile()?.let { Platform.revealInFileManager(it) }
                }
            }

            else -> {}
        }
    }
}
