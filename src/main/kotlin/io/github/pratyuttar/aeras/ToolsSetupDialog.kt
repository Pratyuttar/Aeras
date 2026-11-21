package io.github.pratyuttar.aeras

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.YT_DLP_VERSION
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Seal ships yt-dlp and FFmpeg inside the app. The desktop port uses the standalone
 * programs instead and offers to download them into its own folder on first start.
 */
@Composable
fun ToolsSetupDialog() {
    var needed by remember { mutableStateOf(!Tools.isReady()) }
    val state by Tools.setupState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(needed) {
        if (!needed) {
            withContext(Dispatchers.IO) {
                YoutubeDL.version()?.let { PreferenceUtil.encodeString(YT_DLP_VERSION, it) }
            }
        }
    }

    if (!needed) return

    val missing = remember(state) { Tools.missingDescription() }
    val downloading = state as? Tools.SetupState.Downloading
    val failed = state as? Tools.SetupState.Failed

    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Outlined.Download, null) },
        title = { Text("One-time setup") },
        text = {
            Column {
                if (downloading != null) {
                    Text("Downloading ${downloading.what}…")
                    if (downloading.fraction >= 0f) {
                        LinearProgressIndicator(
                            progress = { downloading.fraction },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    }
                } else {
                    Text(
                        if (Tools.canAutoInstall())
                            "${AppInfo.NAME} needs $missing to download and process media. They can be downloaded " +
                                "now from their official GitHub releases (about 115 MB in total) into " +
                                "this app's own folder:\n${AppDirs.bin.absolutePath}"
                        else
                            "${AppInfo.NAME} needs $missing, which was not found on this computer. Install it " +
                                "and make sure it is on the PATH, then start the app again."
                    )
                    if (failed != null) {
                        Text(
                            failed.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (downloading == null && Tools.canAutoInstall()) {
                Button(
                    onClick = {
                        scope.launch {
                            if (Tools.install()) {
                                needed = !Tools.isReady()
                            }
                        }
                    }
                ) {
                    Text(if (failed != null) stringResource(R.string.restart) else stringResource(R.string.download))
                }
            }
        },
        dismissButton = {
            if (downloading == null) {
                TextButton(onClick = { needed = false }) { Text(stringResource(R.string.dismiss)) }
            }
        },
    )
}
