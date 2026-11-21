// Minimal desktop stand-ins for the android.content classes Seal's code refers to.
package android.content

import android.net.Uri
import io.github.pratyuttar.aeras.Strings
import java.io.File

abstract class Context {
    abstract val applicationContext: Context
    abstract val packageName: String
    abstract val filesDir: File
    abstract val cacheDir: File
    abstract val dataDir: File

    fun getString(id: Int): String = Strings.get(id)

    fun getString(id: Int, vararg formatArgs: Any?): String = Strings.get(id, *formatArgs)

    abstract fun startActivity(intent: Intent)

    val contentResolver: ContentResolver
        get() = ContentResolver

    companion object {
        const val BIND_AUTO_CREATE = 1
    }
}

/** A plain data holder. [Context.startActivity] interprets the common cases on desktop. */
class Intent() {
    var action: String? = null
    var data: Uri? = null
    var type: String? = null
    var flags: Int = 0
    val extras: MutableMap<String, Any?> = mutableMapOf()
    var chooserTitle: CharSequence? = null

    constructor(action: String) : this() {
        this.action = action
    }

    constructor(action: String, uri: Uri?) : this() {
        this.action = action
        this.data = uri
    }

    val dataString: String?
        get() = data?.toString()

    fun putExtra(name: String, value: String?): Intent = apply { extras[name] = value }

    fun putExtra(name: String, value: Uri?): Intent = apply { extras[name] = value }

    fun putExtra(name: String, value: Int): Intent = apply { extras[name] = value }

    fun putExtra(name: String, value: Boolean): Intent = apply { extras[name] = value }

    fun getStringExtra(name: String): String? = extras[name] as? String

    fun addFlags(flags: Int): Intent = apply { this.flags = this.flags or flags }

    fun setDataAndType(data: Uri?, type: String?): Intent = apply {
        this.data = data
        this.type = type
    }

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_TITLE = "android.intent.extra.TITLE"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001
        const val FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002
        const val FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040

        fun createChooser(target: Intent, title: CharSequence?): Intent =
            target.apply { chooserTitle = title }
    }
}

/** Uris are plain file paths on desktop, so "resolving content" is ordinary file access. */
object ContentResolver {
    fun openInputStream(uri: Uri): java.io.InputStream? =
        runCatching { uri.toFile()?.inputStream() }.getOrNull()

    fun openOutputStream(uri: Uri): java.io.OutputStream? =
        runCatching {
                uri.toFile()?.let {
                    it.parentFile?.mkdirs()
                    it.outputStream()
                }
            }
            .getOrNull()

    fun takePersistableUriPermission(uri: Uri, modeFlags: Int) {}

    fun getType(uri: Uri): String? = null
}
