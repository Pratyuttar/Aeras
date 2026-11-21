package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.pratyuttar.aeras.DesktopContext

val LocalContext = staticCompositionLocalOf<Context> { DesktopContext }

val LocalView = staticCompositionLocalOf { View() }

val LocalConfiguration = compositionLocalOf { Configuration() }
