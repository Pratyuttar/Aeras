package io.github.pratyuttar.aeras

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/** Desktop version of Android's Toast: a short message shown near the bottom of the window. */
object DesktopToast {
    data class Message(val id: Long, val text: String, val durationMs: Long)

    private val counter = AtomicLong()
    val current = MutableStateFlow<Message?>(null)

    fun show(text: String, durationMs: Long = 2000L) {
        if (text.isBlank()) return
        current.value = Message(counter.incrementAndGet(), text, durationMs)
    }
}

@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val message by DesktopToast.current.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<DesktopToast.Message?>(null) }

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        shown = m
        visible = true
        delay(m.durationMs)
        visible = false
    }

    Box(modifier = modifier.fillMaxSize().padding(bottom = 72.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 520.dp),
            ) {
                Text(
                    text = shown?.text ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}
