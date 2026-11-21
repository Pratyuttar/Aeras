package android.view

object HapticFeedbackConstants {
    const val LONG_PRESS = 0
    const val VIRTUAL_KEY = 1
    const val KEYBOARD_TAP = 3
    const val CLOCK_TICK = 4
    const val CONTEXT_CLICK = 6
    const val CONFIRM = 16
    const val REJECT = 17
}

/** There is no haptic feedback on a desktop; calls are accepted and ignored. */
class View {
    fun performHapticFeedback(feedbackConstant: Int): Boolean = false
}
