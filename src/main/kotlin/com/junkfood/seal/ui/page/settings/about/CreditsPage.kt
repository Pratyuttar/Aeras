package com.junkfood.seal.ui.page.settings.about

import io.github.pratyuttar.aeras.AppInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.CreditItem
import com.junkfood.seal.ui.svg.DynamicColorImageVectors
import com.junkfood.seal.ui.svg.drawablevectors.coder

data class Credit(val title: String = "", val license: String? = null, val url: String = "")

private const val GPL_V3 = "GNU General Public License v3.0"
private const val GPL_V2 = "GNU General Public License v2.0"
private const val LGPL_V2_1 = "GNU Lesser General Public License, version 2.1"
private const val APACHE_V2 = "Apache License, Version 2.0"
private const val UNLICENSE = "The Unlicense"
private const val BSD = "BSD 3-Clause License"

private const val ytdlpUrl = "https://github.com/yt-dlp/yt-dlp"
private const val readYou = "https://github.com/Ashinch/ReadYou"
private const val materialIcon = "https://fonts.google.com/icons"
private const val materialColor = "https://github.com/material-foundation/material-color-utilities"
private const val monet = "https://github.com/Kyant0/Monet"
private const val jetpack = "https://github.com/androidx/androidx"
private const val coil = "https://github.com/coil-kt/coil"
private const val kotlin = "https://kotlinlang.org/"
private const val okhttp = "https://github.com/square/okhttp"
private const val aria2 = "https://github.com/aria2/aria2"
private const val material3 = "https://m3.material.io/"
private const val unDraw = "https://undraw.co/"
private const val materialMotionCompose = "https://github.com/fornewid/material-motion-compose"
private const val FFmpeg = "https://ffmpeg.org/"
private const val composeMultiplatform = "https://github.com/JetBrains/compose-multiplatform"
private const val koin = "https://github.com/InsertKoinIO/koin"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    val creditsList =
        listOf(
            Credit(AppInfo.UPSTREAM_NAME + " (the app this one is built from)", GPL_V3, AppInfo.UPSTREAM_URL),
            Credit("yt-dlp", UNLICENSE, ytdlpUrl),
            Credit("FFmpeg", GPL_V2, FFmpeg),
            Credit("Read You", GPL_V3, readYou),
            Credit("Compose Multiplatform", APACHE_V2, composeMultiplatform),
            Credit("Android Jetpack", APACHE_V2, jetpack),
            Credit("Kotlin", APACHE_V2, kotlin),
            Credit("Material Design 3", APACHE_V2, material3),
            Credit("Material Icons", APACHE_V2, materialIcon),
            Credit("Monet", APACHE_V2, monet),
            Credit("Material color utilities", APACHE_V2, materialColor),
            Credit("Coil", APACHE_V2, coil),
            Credit("Koin", APACHE_V2, koin),
            Credit("aria2", GPL_V2, aria2),
            Credit("OkHttp", APACHE_V2, okhttp),
            Credit("Java Native Access", APACHE_V2, "https://github.com/java-native-access/jna"),
            Credit("material-motion-compose", APACHE_V2, materialMotionCompose),
            Credit("unDraw", null, unDraw),
        )
    val uriHandler = LocalUriHandler.current
    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(modifier = Modifier, text = stringResource(id = R.string.credits)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            LazyColumn(modifier = Modifier.padding(it)) {
                item {
                    Surface(
                        modifier =
                            Modifier.fillParentMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .clip(MaterialTheme.shapes.large)
                                .clickable {}
                                .clearAndSetSemantics {},
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        val painter =
                            rememberVectorPainter(image = DynamicColorImageVectors.coder())
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 72.dp, vertical = 48.dp),
                        )
                    }
                }
                items(creditsList) { item ->
                    CreditItem(title = item.title, license = item.license) { openUrl(item.url) }
                }
            }
        },
    )
}
