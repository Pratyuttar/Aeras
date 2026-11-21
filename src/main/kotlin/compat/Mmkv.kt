package com.tencent.mmkv

import android.content.Context
import io.github.pratyuttar.aeras.AppDirs
import io.github.pratyuttar.aeras.DebouncedFileWriter
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Desktop stand-in for Tencent's MMKV key-value store, exposing the calls Seal makes. Values are
 * kept in memory and saved to a JSON file in the app's data folder.
 */
class MMKV private constructor(file: File) {
    private val json = Json { prettyPrint = true }
    private val values = ConcurrentHashMap<String, JsonElement>()
    private val writer = DebouncedFileWriter(file)

    init {
        runCatching {
                if (file.exists()) {
                    (json.parseToJsonElement(file.readText()) as? JsonObject)?.forEach { (k, v) ->
                        values[k] = v
                    }
                }
            }
            .onFailure { it.printStackTrace() }
    }

    private fun put(key: String, value: JsonElement): Boolean {
        values[key] = value
        writer.schedule { json.encodeToString(JsonObject.serializer(), JsonObject(HashMap(values).toSortedMap())) }
        return true
    }

    private fun primitive(key: String): JsonPrimitive? = values[key] as? JsonPrimitive

    fun encode(key: String, value: Boolean): Boolean = put(key, JsonPrimitive(value))

    fun encode(key: String, value: Int): Boolean = put(key, JsonPrimitive(value))

    fun encode(key: String, value: Long): Boolean = put(key, JsonPrimitive(value))

    fun encode(key: String, value: Float): Boolean = put(key, JsonPrimitive(value))

    fun encode(key: String, value: String?): Boolean =
        if (value == null) {
            removeValueForKey(key)
            true
        } else put(key, JsonPrimitive(value))

    fun encode(key: String, value: Set<String>?): Boolean =
        if (value == null) {
            removeValueForKey(key)
            true
        } else put(key, JsonArray(value.map { JsonPrimitive(it) }))

    fun decodeBool(key: String, defaultValue: Boolean = false): Boolean =
        primitive(key)?.booleanOrNull ?: defaultValue

    fun decodeInt(key: String, defaultValue: Int = 0): Int =
        primitive(key)?.let { it.intOrNull ?: it.longOrNull?.toInt() } ?: defaultValue

    fun decodeLong(key: String, defaultValue: Long = 0L): Long =
        primitive(key)?.longOrNull ?: defaultValue

    fun decodeFloat(key: String, defaultValue: Float = 0f): Float =
        primitive(key)?.content?.toFloatOrNull() ?: defaultValue

    fun decodeString(key: String): String? = primitive(key)?.takeIf { it.isString }?.content

    fun decodeString(key: String, defaultValue: String?): String? = decodeString(key) ?: defaultValue

    fun decodeStringSet(key: String): Set<String>? =
        (values[key] as? JsonArray)?.map { it.jsonPrimitive.content }?.toSet()

    fun containsKey(key: String): Boolean = values.containsKey(key)

    fun removeValueForKey(key: String) {
        if (values.remove(key) != null) {
            writer.schedule {
                json.encodeToString(JsonObject.serializer(), JsonObject(HashMap(values).toSortedMap()))
            }
        }
    }

    fun flush() = writer.flush()

    companion object {
        private val instances = ConcurrentHashMap<String, MMKV>()

        fun initialize(context: Context?): String = AppDirs.data.absolutePath

        fun defaultMMKV(): MMKV = mmkvWithID("settings")

        fun mmkvWithID(id: String): MMKV =
            instances.getOrPut(id) { MMKV(File(AppDirs.data, "$id.json")) }
    }
}
