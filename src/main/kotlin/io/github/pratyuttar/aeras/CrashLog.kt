package io.github.pratyuttar.aeras

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

/** Uncaught errors are appended to crash.log in the app's data folder. */
object CrashLog {
    val file: File
        get() = File(AppDirs.data, "crash.log")

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, error -> record(thread.name, error) }
    }

    fun record(where: String, error: Throwable) {
        runCatching {
            error.printStackTrace()
            val sw = StringWriter()
            error.printStackTrace(PrintWriter(sw))
            if (file.length() > 2_000_000) file.delete()
            file.appendText("---- ${LocalDateTime.now()} [$where]\n$sw\n")
        }
    }
}
