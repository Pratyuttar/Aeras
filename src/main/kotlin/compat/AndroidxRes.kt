package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.painter.Painter
import io.github.pratyuttar.aeras.DesktopResources
import io.github.pratyuttar.aeras.Strings

@Composable
@ReadOnlyComposable
fun stringResource(id: Int): String {
    Strings.localeState.value // re-read when the app language changes
    return Strings.get(id)
}

@Composable
@ReadOnlyComposable
fun stringResource(id: Int, vararg formatArgs: Any): String {
    Strings.localeState.value
    return Strings.get(id, *formatArgs)
}

@Composable
@ReadOnlyComposable
fun pluralStringResource(id: Int, count: Int): String {
    Strings.localeState.value
    return Strings.getQuantity(id, count)
}

@Composable
@ReadOnlyComposable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String {
    Strings.localeState.value
    return Strings.getQuantity(id, count, *formatArgs)
}

/** Android vector XML, PNG and WebP drawables are loaded from the classpath copy of Seal's resources. */
@Suppress("DEPRECATION")
@Composable
fun painterResource(id: Int): Painter = painterResource(DesktopResources.drawablePath(id))
