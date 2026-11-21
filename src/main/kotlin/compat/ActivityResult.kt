// Desktop stand-ins for Android's "activity result" pickers: they open native file dialogs.
package androidx.activity.result.contract

import android.net.Uri
import io.github.pratyuttar.aeras.FilePickers

abstract class ActivityResultContract<I, O> {
    abstract fun launchOnDesktop(input: I): O
}

class ActivityResultContracts private constructor() {
    /** Input: suggested file name. Result: the file to write, or null when cancelled. */
    class CreateDocument(private val mimeType: String = "*/*") : ActivityResultContract<String, Uri?>() {
        override fun launchOnDesktop(input: String): Uri? =
            FilePickers.saveFile(suggestedName = input)?.let { Uri.fromFile(it) }
    }

    /** Input: MIME type. Result: the chosen file, or null when cancelled. */
    class GetContent : ActivityResultContract<String, Uri?>() {
        override fun launchOnDesktop(input: String): Uri? =
            FilePickers.openFile()?.let { Uri.fromFile(it) }
    }

    /** Input: starting folder (optional). Result: the chosen folder, or null when cancelled. */
    open class OpenDocumentTree : ActivityResultContract<Uri?, Uri?>() {
        override fun launchOnDesktop(input: Uri?): Uri? =
            FilePickers.chooseDirectory(initial = input?.toFile())?.let { Uri.fromFile(it) }
    }
}
