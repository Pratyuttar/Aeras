package android.util

object Log {
    var enabled: Boolean = System.getProperty("aeras.debug") == "true"

    fun d(tag: String?, msg: String?): Int {
        if (enabled) println("D/$tag: $msg")
        return 0
    }

    fun i(tag: String?, msg: String?): Int {
        if (enabled) println("I/$tag: $msg")
        return 0
    }

    fun w(tag: String?, msg: String?): Int {
        System.err.println("W/$tag: $msg")
        return 0
    }

    fun e(tag: String?, msg: String?): Int {
        System.err.println("E/$tag: $msg")
        return 0
    }

    fun e(tag: String?, msg: String?, tr: Throwable?): Int {
        System.err.println("E/$tag: $msg")
        tr?.printStackTrace()
        return 0
    }
}
