// Desktop implementation of the small part of the youtubedl-android API that Seal uses. Instead of
// the Python runtime bundled inside the Android app, it runs the standalone yt-dlp program.
package com.yausername.youtubedl_android

import android.content.Context
import io.github.pratyuttar.aeras.AppDirs
import io.github.pratyuttar.aeras.Tools
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class YoutubeDLException(message: String, cause: Throwable? = null) : Exception(message, cause)

class YoutubeDLResponse(
    val command: List<String>,
    val exitCode: Int,
    val elapsedTime: Long,
    val out: String,
    val err: String,
)

class YoutubeDLRequest {
    private val urls: List<String>
    private val options = LinkedHashMap<String, MutableList<String>>()
    private val customCommandList = ArrayList<String>()

    constructor(url: String) {
        urls = listOf(url)
    }

    constructor(urls: List<String>) {
        this.urls = urls
    }

    fun addOption(option: String, argument: String): YoutubeDLRequest {
        options.getOrPut(option) { ArrayList() }.add(argument)
        return this
    }

    fun addOption(option: String, argument: Number): YoutubeDLRequest =
        addOption(option, argument.toString())

    fun addOption(option: String): YoutubeDLRequest {
        if (!options.containsKey(option)) options[option] = arrayListOf("")
        return this
    }

    fun addCommands(commands: List<String>): YoutubeDLRequest {
        customCommandList.addAll(commands)
        return this
    }

    fun getOption(option: String): String? = options[option]?.firstOrNull()

    fun getArguments(option: String): List<String?>? = options[option]

    fun hasOption(option: String): Boolean = options.containsKey(option)

    fun buildCommand(): List<String> {
        val commandList = ArrayList<String>()
        for ((option, arguments) in options) {
            for (argument in arguments) {
                commandList.add(option)
                if (argument.isNotEmpty()) commandList.add(argument)
            }
        }
        commandList.addAll(customCommandList)
        commandList.addAll(urls)
        return commandList
    }
}

object YoutubeDL {
    class CanceledException : Exception()

    enum class UpdateStatus {
        DONE,
        ALREADY_UP_TO_DATE,
    }

    sealed class UpdateChannel(val channelName: String) {
        object STABLE : UpdateChannel("stable")

        object NIGHTLY : UpdateChannel("nightly")

        object MASTER : UpdateChannel("master")
    }

    private val idProcessMap: MutableMap<String, Process> = ConcurrentHashMap()

    @Volatile private var cachedVersion: String? = null

    @JvmStatic fun getInstance(): YoutubeDL = this

    fun init(appContext: Context?) {
        // Nothing to unpack on desktop; see Tools for locating or downloading yt-dlp.
    }

    private val progressPattern =
        Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d)% .* ETA (\\d+):(\\d+)")
    private val aria2cPattern =
        Pattern.compile("\\[#\\w{6}.*\\((\\d*\\.*\\d+)%\\).*?((\\d+)m)*((\\d+)s)*]")

    private class ProgressState {
        var progress = -1f
        var eta = -1L
    }

    private fun parseLine(line: String, state: ProgressState) {
        val m = progressPattern.matcher(line)
        if (m.find()) {
            state.progress = m.group(1)?.toFloatOrNull() ?: state.progress
            val min = m.group(2)?.toLongOrNull() ?: 0L
            val sec = m.group(3)?.toLongOrNull() ?: 0L
            state.eta = min * 60 + sec
            return
        }
        val a = aria2cPattern.matcher(line)
        if (a.find()) {
            state.progress = a.group(1)?.toFloatOrNull() ?: state.progress
            val min = a.group(3)?.toLongOrNull() ?: 0L
            val sec = a.group(5)?.toLongOrNull() ?: 0L
            state.eta = min * 60 + sec
        }
    }

    private fun readStdout(
        stream: InputStream,
        buffer: StringBuffer,
        callback: ((Float, Long, String) -> Unit)?,
    ) {
        val state = ProgressState()
        val current = StringBuilder()
        InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            val chunk = CharArray(8192)
            while (true) {
                val n = reader.read(chunk)
                if (n < 0) break
                buffer.append(chunk, 0, n)
                if (callback == null) continue
                for (i in 0 until n) {
                    val c = chunk[i]
                    if (c == '\r' || c == '\n') {
                        if (current.isNotEmpty()) {
                            val line = current.toString()
                            current.setLength(0)
                            parseLine(line, state)
                            runCatching { callback(state.progress, state.eta, line) }
                        }
                    } else current.append(c)
                }
            }
        }
        if (callback != null && current.isNotEmpty()) {
            val line = current.toString()
            parseLine(line, state)
            runCatching { callback(state.progress, state.eta, line) }
        }
    }

    private fun baseCommand(): MutableList<String> {
        val ytDlp =
            Tools.ytDlpPath()
                ?: throw YoutubeDLException(
                    "yt-dlp was not found. Open the app's setup dialog or install yt-dlp."
                )
        return mutableListOf(ytDlp)
    }

    private fun newProcess(command: List<String>): Process {
        val builder = ProcessBuilder(command)
        val env = builder.environment()
        val pathKey = env.keys.firstOrNull { it.equals("PATH", ignoreCase = true) } ?: "PATH"
        env[pathKey] = AppDirs.bin.absolutePath + File.pathSeparator + (env[pathKey] ?: "")
        env["PYTHONIOENCODING"] = "utf-8"
        env["PYTHONUTF8"] = "1"
        // yt-dlp must never wait for keyboard input: give it an already-closed stdin
        return builder.start().also { runCatching { it.outputStream.close() } }
    }

    @Throws(YoutubeDLException::class, InterruptedException::class, CanceledException::class)
    fun execute(
        request: YoutubeDLRequest,
        processId: String? = null,
        callback: ((Float, Long, String) -> Unit)? = null,
    ): YoutubeDLResponse {
        if (processId != null && idProcessMap.containsKey(processId))
            throw YoutubeDLException("Process ID already exists")

        val command = baseCommand()
        command.addAll(listOf("--encoding", "utf-8"))
        if (!request.hasOption("--ffmpeg-location")) {
            Tools.ffmpegLocation()?.let { command.addAll(listOf("--ffmpeg-location", it)) }
        }
        command.addAll(request.buildCommand())

        val startTime = System.currentTimeMillis()
        val process =
            try {
                newProcess(command)
            } catch (e: Exception) {
                throw YoutubeDLException(e.message ?: "Failed to start yt-dlp", e)
            }
        if (processId != null) idProcessMap[processId] = process

        val outBuffer = StringBuffer()
        val errBuffer = StringBuffer()
        val outThread = Thread({ runCatching { readStdout(process.inputStream, outBuffer, callback) } }, "yt-dlp-stdout")
        val errThread =
            Thread(
                {
                    runCatching {
                        InputStreamReader(process.errorStream, StandardCharsets.UTF_8).use { r ->
                            val chunk = CharArray(4096)
                            while (true) {
                                val n = r.read(chunk)
                                if (n < 0) break
                                errBuffer.append(chunk, 0, n)
                            }
                        }
                    }
                },
                "yt-dlp-stderr",
            )
        outThread.isDaemon = true
        errThread.isDaemon = true
        outThread.start()
        errThread.start()

        val exitCode: Int =
            try {
                val code = process.waitFor()
                outThread.join()
                errThread.join()
                code
            } catch (e: InterruptedException) {
                killTree(process)
                if (processId != null) idProcessMap.remove(processId)
                throw e
            }

        val out = outBuffer.toString()
        val err = errBuffer.toString()

        if (exitCode != 0) {
            if (processId != null && !idProcessMap.containsKey(processId)) throw CanceledException()
            if (!ignoreErrors(request, out)) {
                if (processId != null) idProcessMap.remove(processId)
                throw YoutubeDLException(err.ifBlank { "yt-dlp exited with code $exitCode" })
            }
        }
        if (processId != null) idProcessMap.remove(processId)

        return YoutubeDLResponse(command, exitCode, System.currentTimeMillis() - startTime, out, err)
    }

    private fun ignoreErrors(request: YoutubeDLRequest, out: String): Boolean =
        request.hasOption("--dump-json") && out.isNotEmpty() && request.hasOption("--ignore-errors")

    private fun killTree(process: Process) {
        runCatching { process.descendants().forEach { it.destroyForcibly() } }
        runCatching { process.destroyForcibly() }
    }

    fun destroyProcessById(id: String): Boolean {
        val process = idProcessMap.remove(id) ?: return false
        killTree(process)
        return true
    }

    /** Runs yt-dlp with plain arguments and returns exit code and combined output. */
    internal fun runSimple(arguments: List<String>, timeoutSeconds: Long = 600): Pair<Int, String> {
        val command = baseCommand()
        command.addAll(arguments)
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        val process = builder.start().also { runCatching { it.outputStream.close() } }
        val output = StringBuffer()
        val reader =
            Thread {
                runCatching {
                    InputStreamReader(process.inputStream, StandardCharsets.UTF_8).use { r ->
                        val chunk = CharArray(4096)
                        while (true) {
                            val n = r.read(chunk)
                            if (n < 0) break
                            output.append(chunk, 0, n)
                        }
                    }
                }
            }
        reader.isDaemon = true
        reader.start()
        if (!process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
            killTree(process)
            throw YoutubeDLException("yt-dlp did not respond in time")
        }
        reader.join(2000)
        return process.exitValue() to output.toString()
    }

    fun version(appContext: Context? = null): String? {
        cachedVersion?.let { return it }
        return runCatching {
                val (code, out) = runSimple(listOf("--version"), timeoutSeconds = 60)
                if (code == 0) out.trim().lines().lastOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                else null
            }
            .getOrNull()
            ?.also { cachedVersion = it }
    }

    fun versionName(appContext: Context? = null): String? = version(appContext)

    internal fun invalidateVersion() {
        cachedVersion = null
    }

    /** Uses yt-dlp's built-in self-updater. */
    @Throws(YoutubeDLException::class)
    fun updateYoutubeDL(
        appContext: Context? = null,
        updateChannel: UpdateChannel = UpdateChannel.STABLE,
    ): UpdateStatus? {
        val before = version()
        val (code, out) = runSimple(listOf("--update-to", updateChannel.channelName))
        invalidateVersion()
        val after = version()
        return when {
            code != 0 -> throw YoutubeDLException(out.trim().ifEmpty { "Update failed" })
            before != null && after != null && before != after -> UpdateStatus.DONE
            out.contains("Updated yt-dlp", ignoreCase = true) -> UpdateStatus.DONE
            else -> UpdateStatus.ALREADY_UP_TO_DATE
        }
    }

    internal fun runningProcessIds(): Set<String> = Collections.unmodifiableSet(idProcessMap.keys)
}
