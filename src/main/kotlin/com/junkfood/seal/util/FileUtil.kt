package com.junkfood.seal.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.CheckResult
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.R
import io.github.pratyuttar.aeras.AppDirs
import io.github.pratyuttar.aeras.AppInfo
import java.io.File

const val AUDIO_REGEX = "(mp3|aac|opus|m4a)$"
const val THUMBNAIL_REGEX = "\\.(jpg|png)$"
const val SUBTITLE_REGEX = "\\.(lrc|vtt|srt|ass|json3|srv.|ttml)$"
private const val PRIVATE_DIRECTORY_SUFFIX = "." + AppInfo.NAME

/** Desktop version: plain files instead of Android's content URIs and media scanner. */
object FileUtil {
    fun openFileFromResult(downloadResult: Result<List<String>>) {
        val filePaths = downloadResult.getOrNull()
        if (filePaths.isNullOrEmpty()) return
        openFile(filePaths.first()) {
            ToastUtil.makeToastSuspend(context.getString(R.string.file_unavailable))
        }
    }

    inline fun openFile(path: String, onFailureCallback: (Throwable) -> Unit) =
        path
            .runCatching {
                createIntentForOpeningFile(this)?.run { context.startActivity(this) }
                    ?: throw Exception()
            }
            .onFailure { onFailureCallback(it) }

    private fun createIntentForFile(path: String?): Intent? {
        if (path == null) return null
        val file = File(path)
        if (!file.exists()) return null
        return Intent().apply { data = Uri.fromFile(file) }
    }

    fun createIntentForOpeningFile(path: String?): Intent? =
        createIntentForFile(path)?.apply { action = Intent.ACTION_VIEW }

    fun createIntentForSharingFile(path: String?): Intent? =
        createIntentForFile(path)?.apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, data)
        }

    fun String.getFileSize(): Long = File(this).length()

    fun String.getFileName(): String = File(this).nameWithoutExtension.ifEmpty { "video" }

    fun deleteFile(path: String) = path.runCatching { File(path).delete() }

    /**
     * Lists the files a finished download produced (thumbnails and subtitles excluded).
     *
     * By default the audio folder sits inside the video folder, so a search of the video folder
     * for a title would also find an earlier audio download of the same title, and the queue
     * would then open the wrong file. The other media folder is therefore skipped when it is
     * nested inside the folder being searched, and newer files are listed first.
     */
    @CheckResult
    fun scanFileToMediaLibraryPostDownload(title: String, downloadDir: String): List<String> {
        val root = File(downloadDir).absoluteFile
        val otherMediaDirs =
            listOf(App.videoDownloadDir, App.audioDownloadDir)
                .map { File(it).absoluteFile }
                .filter { it != root && it.startsWith(root) }
        return root
            .walkTopDown()
            .onEnter { dir -> dir.absoluteFile !in otherMediaDirs }
            .filter { it.isFile && it.absolutePath.contains(title) }
            .sortedByDescending { it.lastModified() }
            .map { it.absolutePath }
            .toMutableList()
            .apply {
                removeAll {
                    it.contains(Regex(THUMBNAIL_REGEX)) || it.contains(Regex(SUBTITLE_REGEX))
                }
            }
    }

    fun scanDownloadDirectoryToMediaLibrary(downloadDir: String) {
        // Android's media scanner has no desktop equivalent; nothing to do.
    }

    @CheckResult
    fun moveFilesToSdcard(tempPath: File, sdcardUri: String): Result<List<String>> =
        Result.failure(UnsupportedOperationException("SD card downloads are an Android feature"))

    fun clearTempFiles(downloadDir: File): Int {
        var count = 0
        downloadDir.walkTopDown().forEach {
            if (it.isFile && !it.isHidden) {
                if (it.delete()) count++
            }
        }
        return count
    }

    fun Context.getConfigDirectory(): File = cacheDir

    fun Context.getConfigFile(suffix: String = "") =
        File(getConfigDirectory(), "config${suffix.sanitizeForFileName()}.txt")

    fun Context.getCookiesFile() = File(getConfigDirectory(), "cookies.txt")

    fun getExternalTempDir() = File(AppDirs.cache, "tmp").apply { mkdirs() }

    fun Context.getSdcardTempDir(child: String?): File =
        getExternalTempDir().run { child?.let { resolve(it) } ?: this }

    fun Context.getArchiveFile(): File = filesDir.createEmptyFile("archive.txt").getOrThrow()

    fun Context.getInternalTempDir() = File(filesDir, "tmp")

    internal fun getExternalDownloadDirectory() =
        File(AppDirs.systemDownloads, AppInfo.NAME).also { it.mkdirs() }

    internal fun getExternalPrivateDownloadDirectory() =
        File(AppDirs.systemDownloads, PRIVATE_DIRECTORY_SUFFIX)

    fun File.createEmptyFile(fileName: String): Result<File> =
        this.runCatching {
                mkdirs()
                resolve(fileName).apply { this@apply.createNewFile() }
            }
            .onFailure { it.printStackTrace() }

    fun writeContentToFile(content: String, file: File): File =
        file.apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    /** On desktop a "tree URI" is simply the chosen folder's path. */
    fun getRealPath(treeUri: Uri): String = treeUri.path ?: getExternalDownloadDirectory().absolutePath

    private fun String.sanitizeForFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    private const val TAG = "FileUtil"
}
