package com.junkfood.seal.ui.page.settings.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import io.github.pratyuttar.aeras.FilePickers
import io.github.pratyuttar.aeras.Platform
import com.junkfood.seal.util.COOKIES
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.DownloadUtil.toCookiesFileContent
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Desktop replacement for Seal's in-app browser page. Android Seal logs in inside a WebView and
 * reads the cookies back; on a PC the cookies come from a `cookies.txt` exported from the user's
 * own browser, which is imported here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPage(cookiesViewModel: CookiesViewModel, onDismissRequest: () -> Unit) {
    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    val websiteUrl = state.editingCookieProfile.url
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun import(text: String?) {
        scope.launch {
            val added =
                withContext(Dispatchers.IO) {
                    val incoming = DownloadUtil.parseNetscapeCookies(text ?: "")
                    if (incoming.isEmpty()) return@withContext 0
                    val existing = DownloadUtil.getCookieListFromDatabase().getOrNull() ?: emptyList()
                    val merged = LinkedHashMap<Triple<String, String, String>, Cookie>()
                    (existing + incoming).forEach { merged[Triple(it.domain, it.path, it.name)] = it }
                    FileUtil.writeContentToFile(
                        merged.values.toList().toCookiesFileContent(),
                        context.getCookiesFile(),
                    )
                    incoming.size
                }
            if (added > 0) {
                COOKIES.updateBoolean(true)
                ToastUtil.makeToast("Imported $added cookies")
                onDismissRequest()
            } else {
                ToastUtil.makeToast("No cookies found. The file must be in Netscape cookies.txt format.")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cookies), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { onDismissRequest() }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text =
                    "On a PC, cookies come from your own browser:\n\n" +
                        "1. Open the site in your browser and sign in.\n" +
                        "2. Export the cookies as a cookies.txt file (Netscape format) with a " +
                        "browser extension such as \"Get cookies.txt LOCALLY\".\n" +
                        "3. Import that file here.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(max = 640.dp),
            )
            if (websiteUrl.startsWith("http") && websiteUrl.substringAfter("://").isNotBlank()) {
                OutlinedButton(onClick = { Platform.openUrl(websiteUrl) }) {
                    Icon(Icons.Outlined.OpenInBrowser, null)
                    Text(websiteUrl, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
                }
            }
            Button(
                onClick = {
                    val file = FilePickers.openFile(title = "cookies.txt") ?: return@Button
                    import(runCatching { file.readText() }.getOrNull())
                }
            ) {
                Icon(Icons.Outlined.FileOpen, null)
                Text(
                    stringResource(R.string.import_from) + " cookies.txt",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = { import(Platform.readClipboardText()) }) {
                Icon(Icons.Outlined.ContentPaste, null)
                Text(
                    stringResource(R.string.import_from_clipboard),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text =
                    "The cookies are stored only in this app's data folder and are passed to yt-dlp " +
                        "when \"" + stringResource(R.string.use_cookies) + "\" is on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
            )
        }
    }
}
