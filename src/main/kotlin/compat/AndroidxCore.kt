package androidx.core.text

fun CharSequence.isDigitsOnly(): Boolean {
    for (c in this) if (!c.isDigit()) return false
    return true
}
