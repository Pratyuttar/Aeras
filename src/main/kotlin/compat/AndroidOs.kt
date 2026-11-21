package android.os

import java.io.File

/**
 * Seal checks the Android API level in many places. 30 selects the modern code paths while keeping
 * the features that only exist on newer Android (per-app system language page, notification
 * permission prompts, predictive back) switched off.
 */
object Build {
    object VERSION {
        const val SDK_INT: Int = 30
        val RELEASE: String = System.getProperty("os.version") ?: ""
        val RELEASE_OR_CODENAME: String = RELEASE
    }

    object VERSION_CODES {
        const val M = 23
        const val N = 24
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
    }

    val SUPPORTED_ABIS: Array<String> = arrayOf(System.getProperty("os.arch") ?: "")
    val MODEL: String = System.getProperty("os.name") ?: ""
    val MANUFACTURER: String = ""
}

object Environment {
    const val DIRECTORY_DOWNLOADS = "Downloads"

    fun isExternalStorageManager(): Boolean = true

    fun getExternalStorageDirectory(): File = io.github.pratyuttar.aeras.AppDirs.userHome

    fun getExternalStoragePublicDirectory(type: String): File =
        File(io.github.pratyuttar.aeras.AppDirs.userHome, type)
}
