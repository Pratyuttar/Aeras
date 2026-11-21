package io.github.pratyuttar.aeras

import com.junkfood.seal.database.VideoInfoDao
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.database.objects.CookieProfile
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.database.objects.OptionShortcut
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Desktop replacement for Seal's Room database: the same DAO interface, kept in memory and saved
 * to a JSON file. The data sets involved (download history, command templates, cookie profiles,
 * option shortcuts) are small.
 */
class JsonVideoInfoDao(file: File) : VideoInfoDao {

    @Serializable
    private data class Snapshot(
        val history: List<DownloadedVideoInfo> = emptyList(),
        val templates: List<CommandTemplate> = emptyList(),
        val cookies: List<CookieProfile> = emptyList(),
        val shortcuts: List<OptionShortcut> = emptyList(),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val writer = DebouncedFileWriter(file)

    private val state: MutableStateFlow<Snapshot> =
        MutableStateFlow(
            runCatching {
                    if (file.exists()) json.decodeFromString<Snapshot>(file.readText()) else Snapshot()
                }
                .onFailure { it.printStackTrace() }
                .getOrDefault(Snapshot())
        )

    private inline fun mutate(block: (Snapshot) -> Snapshot) {
        state.update(block)
        val snapshot = state.value
        writer.schedule { json.encodeToString(Snapshot.serializer(), snapshot) }
    }

    private fun List<DownloadedVideoInfo>.nextHistoryId() = (maxOfOrNull { it.id } ?: 0) + 1

    // ---- download history ---------------------------------------------------------------------

    override suspend fun insert(info: DownloadedVideoInfo) = mutate { s ->
        val id = if (info.id == 0) s.history.nextHistoryId() else info.id
        s.copy(history = s.history + info.copy(id = id))
    }

    override suspend fun insertAll(infoList: List<DownloadedVideoInfo>) = mutate { s ->
        var next = s.history.nextHistoryId()
        s.copy(history = s.history + infoList.map { if (it.id == 0) it.copy(id = next++) else it })
    }

    override fun getDownloadHistoryFlow(): Flow<List<DownloadedVideoInfo>> =
        state.map { it.history }.distinctUntilChanged()

    override suspend fun getDownloadHistory(): List<DownloadedVideoInfo> = state.value.history

    override suspend fun getInfoById(id: Int): DownloadedVideoInfo =
        state.value.history.first { it.id == id }

    override suspend fun deleteInfoById(id: Int) = mutate { s ->
        s.copy(history = s.history.filterNot { it.id == id })
    }

    override suspend fun deleteInfoByPath(path: String) = mutate { s ->
        s.copy(history = s.history.filterNot { it.videoPath == path })
    }

    override suspend fun getInfoByPath(path: String): DownloadedVideoInfo? =
        state.value.history.firstOrNull { it.videoPath == path }

    override suspend fun deleteInfo(vararg info: DownloadedVideoInfo) {
        val ids = info.map { it.id }.toSet()
        mutate { s -> s.copy(history = s.history.filterNot { it.id in ids }) }
    }

    override suspend fun deleteInfoList(idList: List<DownloadedVideoInfo>) {
        val ids = idList.map { it.id }.toSet()
        mutate { s -> s.copy(history = s.history.filterNot { it.id in ids }) }
    }

    // ---- command templates --------------------------------------------------------------------

    override fun getTemplateFlow(): Flow<List<CommandTemplate>> =
        state.map { it.templates }.distinctUntilChanged()

    override suspend fun getTemplateList(): List<CommandTemplate> = state.value.templates

    override suspend fun insertTemplate(template: CommandTemplate): Long {
        var assigned = template.id
        mutate { s ->
            assigned =
                if (template.id == 0) (s.templates.maxOfOrNull { it.id } ?: 0) + 1 else template.id
            s.copy(templates = s.templates + template.copy(id = assigned))
        }
        return assigned.toLong()
    }

    override suspend fun importTemplates(templateList: List<CommandTemplate>) = mutate { s ->
        var next = (s.templates.maxOfOrNull { it.id } ?: 0) + 1
        s.copy(
            templates = s.templates + templateList.map { if (it.id == 0) it.copy(id = next++) else it }
        )
    }

    override suspend fun updateTemplate(template: CommandTemplate) = mutate { s ->
        s.copy(templates = s.templates.map { if (it.id == template.id) template else it })
    }

    override suspend fun deleteTemplate(template: CommandTemplate) = mutate { s ->
        s.copy(templates = s.templates.filterNot { it.id == template.id })
    }

    override suspend fun getTemplateById(id: Int): CommandTemplate =
        state.value.templates.first { it.id == id }

    override suspend fun deleteTemplateById(id: Int) = mutate { s ->
        s.copy(templates = s.templates.filterNot { it.id == id })
    }

    override suspend fun deleteTemplates(templates: List<CommandTemplate>) {
        val ids = templates.map { it.id }.toSet()
        mutate { s -> s.copy(templates = s.templates.filterNot { it.id in ids }) }
    }

    // ---- cookie profiles ----------------------------------------------------------------------

    override fun getCookieProfileFlow(): Flow<List<CookieProfile>> =
        state.map { it.cookies }.distinctUntilChanged()

    override suspend fun getCookieById(id: Int): CookieProfile? =
        state.value.cookies.firstOrNull { it.id == id }

    override suspend fun updateCookieProfile(cookieProfile: CookieProfile) = mutate { s ->
        s.copy(cookies = s.cookies.map { if (it.id == cookieProfile.id) cookieProfile else it })
    }

    override suspend fun deleteCookieProfile(cookieProfile: CookieProfile) = mutate { s ->
        s.copy(cookies = s.cookies.filterNot { it.id == cookieProfile.id })
    }

    override suspend fun insertCookieProfile(cookieProfile: CookieProfile) = mutate { s ->
        val id =
            if (cookieProfile.id == 0) (s.cookies.maxOfOrNull { it.id } ?: 0) + 1
            else cookieProfile.id
        s.copy(cookies = s.cookies + cookieProfile.copy(id = id))
    }

    // ---- option shortcuts ---------------------------------------------------------------------

    override fun getOptionShortcuts(): Flow<List<OptionShortcut>> =
        state.map { it.shortcuts }.distinctUntilChanged()

    override suspend fun getShortcutList(): List<OptionShortcut> = state.value.shortcuts

    override suspend fun deleteShortcut(optionShortcut: OptionShortcut) = mutate { s ->
        s.copy(shortcuts = s.shortcuts.filterNot { it.id == optionShortcut.id })
    }

    override suspend fun insertShortcut(optionShortcut: OptionShortcut): Long {
        var assigned = optionShortcut.id
        mutate { s ->
            assigned =
                if (optionShortcut.id == 0L) (s.shortcuts.maxOfOrNull { it.id } ?: 0L) + 1
                else optionShortcut.id
            s.copy(shortcuts = s.shortcuts + optionShortcut.copy(id = assigned))
        }
        return assigned
    }

    override suspend fun insertAllShortcuts(shortcuts: List<OptionShortcut>) = mutate { s ->
        var next = (s.shortcuts.maxOfOrNull { it.id } ?: 0L) + 1
        s.copy(shortcuts = s.shortcuts + shortcuts.map { if (it.id == 0L) it.copy(id = next++) else it })
    }
}
