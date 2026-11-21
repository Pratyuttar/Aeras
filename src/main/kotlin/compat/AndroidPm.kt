package android.content.pm

class PackageInfo(val versionName: String, val longVersionCode: Long) {
    val versionCode: Int
        get() = longVersionCode.toInt()
}
