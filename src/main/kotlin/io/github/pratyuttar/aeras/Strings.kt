package io.github.pratyuttar.aeras

import androidx.compose.runtime.mutableStateOf
import com.junkfood.seal.ResourceIndex
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Loads Seal's Android `strings.xml` files (default plus translations) from the classpath and
 * serves them by the ids generated in R.kt.
 */
object Strings {
    private class Table(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private val default: Table by lazy { load("values") ?: Table(emptyMap(), emptyMap()) }

    @Volatile private var localized: Table? = null
    @Volatile private var activeLocale: Locale = Locale.getDefault()

    /** `null` means "follow the system language". Composables read this to recompose on change. */
    val localeState = mutableStateOf<Locale?>(null)

    init {
        applyLocale(null)
    }

    fun setLocale(locale: Locale?) {
        applyLocale(locale)
        localeState.value = locale
    }

    fun currentLocale(): Locale = activeLocale

    private fun applyLocale(locale: Locale?) {
        val target = locale ?: Locale.getDefault()
        activeLocale = target
        localized = candidateDirs(target).firstNotNullOfOrNull { load(it) }
    }

    private fun candidateDirs(locale: Locale): List<String> {
        val lang =
            when (val l = locale.language.lowercase()) {
                "id" -> "in"
                "he" -> "iw"
                "yi" -> "ji"
                else -> l
            }
        if (lang.isEmpty() || lang == "en") return emptyList()
        val country = locale.country.uppercase()
        val script = locale.script
        val result = mutableListOf<String>()
        if (lang == "zh") {
            val traditional =
                script.equals("Hant", ignoreCase = true) || country in setOf("TW", "HK", "MO")
            result += if (traditional) "values-zh-rTW" else "values-zh-rCN"
            return result
        }
        if (country.isNotEmpty()) result += "values-$lang-r$country"
        result += "values-$lang"
        return result.filter { it in ResourceIndex.localeDirs }
    }

    private fun load(dir: String): Table? {
        val stream = Strings::class.java.getResourceAsStream("/aeras/$dir/strings.xml") ?: return null
        return runCatching {
                stream.use { input ->
                    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
                    val strings = HashMap<String, String>()
                    val plurals = HashMap<String, Map<String, String>>()
                    val nodes = doc.documentElement.childNodes
                    for (i in 0 until nodes.length) {
                        val el = nodes.item(i) as? Element ?: continue
                        val name = el.getAttribute("name")
                        when (el.tagName) {
                            "string" -> strings[name] = unescape(el.textContent)
                            "plurals" -> {
                                val items = HashMap<String, String>()
                                val children = el.childNodes
                                for (j in 0 until children.length) {
                                    val item = children.item(j) as? Element ?: continue
                                    items[item.getAttribute("quantity")] = unescape(item.textContent)
                                }
                                plurals[name] = items
                            }
                        }
                    }
                    Table(strings, plurals)
                }
            }
            .onFailure { it.printStackTrace() }
            .getOrNull()
    }

    /** Applies Android's string-resource escaping and whitespace rules. */
    private fun unescape(raw: String): String {
        val s = raw.trim()
        val sb = StringBuilder(s.length)
        var i = 0
        var inQuote = false
        var lastWasSpace = false
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                when (n) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'u' -> {
                        val hex = s.substring(minOf(i + 2, s.length), minOf(i + 6, s.length))
                        val code = hex.toIntOrNull(16)
                        if (code != null && hex.length == 4) {
                            sb.append(code.toChar())
                            i += 4
                        } else sb.append('u')
                    }
                    else -> sb.append(n)
                }
                i += 2
                lastWasSpace = false
                continue
            }
            if (c == '"') {
                inQuote = !inQuote
                i++
                continue
            }
            if (c.isWhitespace() && !inQuote) {
                if (!lastWasSpace) sb.append(' ')
                lastWasSpace = true
            } else {
                sb.append(c)
                lastWasSpace = false
            }
            i++
        }
        return sb.toString()
    }

    private fun raw(id: Int): String {
        val name = ResourceIndex.stringNames.getOrNull(id) ?: return ""
        return localized?.strings?.get(name) ?: default.strings[name] ?: name
    }

    fun get(id: Int): String = raw(id)

    fun get(id: Int, vararg args: Any?): String = format(raw(id), args)

    fun getQuantity(id: Int, count: Int, vararg args: Any?): String {
        val name = ResourceIndex.pluralNames.getOrNull(id - ResourceIndex.PLURALS_BASE) ?: return ""
        val category = pluralCategory(activeLocale.language, count)
        val template =
            localized?.plurals?.get(name)?.let { it[category] ?: it["other"] }
                ?: default.plurals[name]?.let { it[if (count == 1) "one" else "other"] ?: it["other"] }
                ?: name
        return format(template, if (args.isEmpty()) arrayOf(count) else args)
    }

    private fun format(template: String, args: Array<out Any?>): String =
        if (args.isEmpty()) template
        else runCatching { String.format(activeLocale, template, *args) }.getOrDefault(template)

    /** A compact subset of the CLDR plural rules covering the languages Seal is translated into. */
    private fun pluralCategory(language: String, n: Int): String {
        val mod10 = n % 10
        val mod100 = n % 100
        return when (language) {
            "ja", "ko", "zh", "vi", "th", "id", "in", "ms", "km", "tr", "az", "hu" -> "other"
            "fr", "pt", "hi", "bn", "fil", "pa", "si", "kn" -> if (n == 0 || n == 1) "one" else "other"
            "ru", "uk", "be", "sr", "hr" ->
                when {
                    mod10 == 1 && mod100 != 11 -> "one"
                    mod10 in 2..4 && mod100 !in 12..14 -> "few"
                    else -> "many"
                }
            "pl" ->
                when {
                    n == 1 -> "one"
                    mod10 in 2..4 && mod100 !in 12..14 -> "few"
                    else -> "many"
                }
            "cs", "sk" ->
                when {
                    n == 1 -> "one"
                    n in 2..4 -> "few"
                    else -> "other"
                }
            "ar" ->
                when {
                    n == 0 -> "zero"
                    n == 1 -> "one"
                    n == 2 -> "two"
                    mod100 in 3..10 -> "few"
                    mod100 in 11..99 -> "many"
                    else -> "other"
                }
            "he", "iw" -> if (n == 1) "one" else if (n == 2) "two" else "other"
            "lt" ->
                when {
                    mod10 == 1 && mod100 !in 11..19 -> "one"
                    mod10 in 2..9 && mod100 !in 11..19 -> "few"
                    else -> "other"
                }
            "lv" -> if (mod10 == 1 && mod100 != 11) "one" else if (n == 0) "zero" else "other"
            "ro" -> if (n == 1) "one" else if (n == 0 || mod100 in 1..19) "few" else "other"
            "sl" ->
                when (mod100) {
                    1 -> "one"
                    2 -> "two"
                    3, 4 -> "few"
                    else -> "other"
                }
            else -> if (n == 1) "one" else "other"
        }
    }
}
