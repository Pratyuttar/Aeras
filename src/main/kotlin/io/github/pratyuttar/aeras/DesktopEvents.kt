package io.github.pratyuttar.aeras

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-app replacement for the Android intents Seal sends to itself (for example "download this URL
 * again" from the history page).
 */
object DesktopEvents {
    private val mutableDownloadRequests =
        MutableSharedFlow<String>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val downloadRequests = mutableDownloadRequests.asSharedFlow()

    fun requestDownload(url: String) {
        mutableDownloadRequests.tryEmit(url)
    }
}
