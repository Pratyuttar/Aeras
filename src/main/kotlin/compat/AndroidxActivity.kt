package androidx.activity.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/** Routes Android's back handling to Compose's desktop back handler (Escape key). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)
}

class ManagedActivityResultLauncher<I, O>
internal constructor(
    private val contract: androidx.activity.result.contract.ActivityResultContract<I, O>,
    private val onResult: androidx.compose.runtime.State<(O) -> Unit>,
) {
    fun launch(input: I) {
        onResult.value(contract.launchOnDesktop(input))
    }
}

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: androidx.activity.result.contract.ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O> {
    val currentOnResult = androidx.compose.runtime.rememberUpdatedState(onResult)
    return androidx.compose.runtime.remember(contract) {
        ManagedActivityResultLauncher(contract, currentOnResult)
    }
}
