package android.net

import java.io.File

/** On desktop a Uri is just a file path or a URL kept as a string. */
class Uri private constructor(private val value: String) {
    val scheme: String?
        get() = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):").find(value)?.groupValues?.get(1)
            ?.takeIf { it.length > 1 }

    val path: String?
        get() =
            when {
                value.startsWith("file:") -> runCatching { File(java.net.URI(value)).path }.getOrNull()
                scheme == "http" || scheme == "https" ->
                    runCatching { java.net.URI(value).path }.getOrNull()
                else -> value
            }

    val isWebUrl: Boolean
        get() = scheme == "http" || scheme == "https"

    fun toFile(): File? = if (isWebUrl) null else path?.let { File(it) }

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is Uri && other.value == value

    override fun hashCode(): Int = value.hashCode()

    companion object {
        val EMPTY = Uri("")

        fun parse(uriString: String): Uri = Uri(uriString)

        fun fromFile(file: File): Uri = Uri(file.absolutePath)
    }
}

class ConnectivityManager {
    val isActiveNetworkMetered: Boolean
        get() = false
}
