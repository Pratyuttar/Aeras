package android.content.res

class Configuration(
    val screenWidthDp: Int = 0,
    val screenHeightDp: Int = 0,
    val orientation: Int = ORIENTATION_LANDSCAPE,
) {
    companion object {
        const val UI_MODE_NIGHT_NO = 0x10
        const val UI_MODE_NIGHT_YES = 0x20
        const val UI_MODE_NIGHT_MASK = 0x30
        const val UI_MODE_TYPE_NORMAL = 0x01
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
    }
}
