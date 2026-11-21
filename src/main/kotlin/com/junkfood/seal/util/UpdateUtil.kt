package com.junkfood.seal.util

import android.content.Context
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.context
import io.github.pratyuttar.aeras.Tools
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Desktop version. yt-dlp is updated through its own self-updater. Updating the app itself the way
 * the Android build does (downloading an APK) does not apply to this port, so those entry points
 * report that nothing is available.
 */
object UpdateUtil {
    private const val TAG = "UpdateUtil"

    private val updateMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Updates the app's own copy of yt-dlp. A yt-dlp installed elsewhere on the computer (found on
     * the PATH) is never modified: the automatic update skips it, and a manual update first
     * fetches a copy of its own for the app, which is used and updated from then on.
     */
    suspend fun updateYtDlp(manual: Boolean = true): YoutubeDL.UpdateStatus? =
        withContext(Dispatchers.IO) {
            updateMutex.withLock {
                var status: YoutubeDL.UpdateStatus? = null
                if (!Tools.isYtDlpManaged()) {
                    if (!manual) return@withLock null
                    if (!Tools.install(ownCopies = true)) {
                        throw Exception("Could not download yt-dlp")
                    }
                    Tools.resetSetupState()
                    status = YoutubeDL.UpdateStatus.DONE
                }
                val channel =
                    when (YT_DLP_UPDATE_CHANNEL.getInt()) {
                        YT_DLP_NIGHTLY -> YoutubeDL.UpdateChannel.NIGHTLY
                        else -> YoutubeDL.UpdateChannel.STABLE
                    }
                val result =
                    YoutubeDL.getInstance()
                        .updateYoutubeDL(appContext = context, updateChannel = channel)
                YoutubeDL.getInstance().version(context)?.let {
                    PreferenceUtil.encodeString(YT_DLP_VERSION, it)
                }
                YT_DLP_UPDATE_TIME.updateLong(System.currentTimeMillis())
                status ?: result
            }
        }

    fun checkForUpdate(context: Context = App.context): Release? = null

    fun installLatestApk(context: Context = App.context) {}

    suspend fun deleteOutdatedApk(context: Context = App.context) {}

    suspend fun downloadApk(context: Context = App.context, release: Release): Flow<DownloadStatus> =
        emptyFlow()

    @Serializable
    data class Release(
        @SerialName("html_url") val htmlUrl: String? = null,
        @SerialName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val draft: Boolean? = null,
        @SerialName("prerelease") val preRelease: Boolean? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("published_at") val publishedAt: String? = null,
        val assets: List<AssetsItem>? = null,
        val body: String? = null,
    )

    @Serializable
    data class AssetsItem(
        val name: String? = null,
        @SerialName("content_type") val contentType: String? = null,
        val size: Int? = null,
        @SerialName("download_count") val downloadCount: Int? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
        @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
    )

    sealed class DownloadStatus {
        object NotYet : DownloadStatus()

        data class Progress(val percent: Int) : DownloadStatus()

        data class Finished(val file: File) : DownloadStatus()
    }

    private val pattern = Pattern.compile("""v?(\d+)\.(\d+)\.(\d+)(-(\w+)\.(\d+))?""")
    private val EMPTY_VERSION = Version.Stable()

    fun String?.toVersion(): Version =
        this?.run {
            val matcher = pattern.matcher(this)
            if (matcher.find()) {
                val major = matcher.group(1)?.toInt() ?: 0
                val minor = matcher.group(2)?.toInt() ?: 0
                val patch = matcher.group(3)?.toInt() ?: 0
                val buildNumber = matcher.group(6)?.toInt() ?: 0
                when (matcher.group(5)) {
                    "alpha" -> Version.Alpha(major, minor, patch, buildNumber)
                    "beta" -> Version.Beta(major, minor, patch, buildNumber)
                    "rc" -> Version.ReleaseCandidate(major, minor, patch, buildNumber)
                    else -> Version.Stable(major, minor, patch)
                }
            } else EMPTY_VERSION
        } ?: EMPTY_VERSION

    sealed class Version(val major: Int, val minor: Int, val patch: Int, val build: Int = 0) :
        Comparable<Version> {
        companion object {
            // private const val ABI = 1L
            private const val BUILD = 10L
            private const val VARIANT = 100L
            private const val PATCH = 10_000L
            private const val MINOR = 1_000_000L
            private const val MAJOR = 100_000_000L

            private const val STABLE = VARIANT * 4
            private const val ALPHA = VARIANT * 1
            private const val BETA = VARIANT * 2
            private const val RELEASE_CANDIDATE = VARIANT * 3
        }

        abstract fun toVersionName(): String

        abstract fun toNumber(): Long

        class Alpha(
            versionMajor: Int = 0,
            versionMinor: Int = 0,
            versionPatch: Int = 0,
            versionBuild: Int = 0,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-alpha.$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + ALPHA
        }

        class Beta(versionMajor: Int, versionMinor: Int, versionPatch: Int, versionBuild: Int) :
            Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-beta.$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + BETA
        }

        class ReleaseCandidate(
            versionMajor: Int,
            versionMinor: Int,
            versionPatch: Int,
            versionBuild: Int,
        ) : Version(versionMajor, versionMinor, versionPatch, versionBuild) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}-rc.$build"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + RELEASE_CANDIDATE
        }

        class Stable(versionMajor: Int = 0, versionMinor: Int = 0, versionPatch: Int = 0) :
            Version(versionMajor, versionMinor, versionPatch) {
            override fun toVersionName(): String = "${major}.${minor}.${patch}"

            override fun toNumber(): Long =
                major * MAJOR + minor * MINOR + patch * PATCH + build * BUILD + STABLE
            // Prioritize stable versions

        }

        override operator fun compareTo(other: Version): Int =
            this.toNumber().compareTo(other.toNumber())
    }
}
