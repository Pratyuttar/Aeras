package io.github.pratyuttar.aeras

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Writes a text file shortly after it was last changed, replacing the previous version atomically.
 * Used by the settings and history stores so that frequent updates do not hammer the disk.
 */
class DebouncedFileWriter(private val file: File, private val delayMs: Long = 400) {
    private val lock = Any()
    private var pending: (() -> String)? = null
    private var future: ScheduledFuture<*>? = null

    init {
        Runtime.getRuntime().addShutdownHook(Thread { flush() })
    }

    fun schedule(content: () -> String) {
        synchronized(lock) {
            pending = content
            if (future == null || future!!.isDone) {
                future = executor.schedule({ flush() }, delayMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    fun flush() {
        val producer: (() -> String)?
        synchronized(lock) {
            producer = pending
            pending = null
        }
        if (producer == null) return
        runCatching {
                val text = producer()
                file.parentFile?.mkdirs()
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(text, StandardCharsets.UTF_8)
                try {
                    Files.move(
                        tmp.toPath(),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (e: Exception) {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
            .onFailure { it.printStackTrace() }
    }

    companion object {
        private val executor =
            Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "aeras-store-writer").apply { isDaemon = true }
            }
    }
}
