package com.google.accompanist.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@RequiresOptIn annotation class ExperimentalPermissionsApi

sealed interface PermissionStatus {
    object Granted : PermissionStatus

    data class Denied(val shouldShowRationale: Boolean) : PermissionStatus
}

val PermissionStatus.isGranted: Boolean
    get() = this == PermissionStatus.Granted

interface PermissionState {
    val permission: String
    val status: PermissionStatus

    fun launchPermissionRequest()
}

/** Desktop has no runtime permissions: everything is granted. */
@Composable
fun rememberPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit = {},
): PermissionState = remember(permission) {
    object : PermissionState {
        override val permission: String = permission
        override val status: PermissionStatus = PermissionStatus.Granted

        override fun launchPermissionRequest() = onPermissionResult(true)
    }
}
