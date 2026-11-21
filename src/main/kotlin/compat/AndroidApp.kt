package android.app

import android.content.Context
import android.content.Intent

class PendingIntent private constructor(val intent: Intent) {
    companion object {
        const val FLAG_IMMUTABLE = 0x04000000
        const val FLAG_UPDATE_CURRENT = 0x08000000

        fun getActivity(context: Context?, requestCode: Int, intent: Intent, flags: Int): PendingIntent =
            PendingIntent(intent)
    }
}
