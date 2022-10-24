package ru.yakaska.flexplayer.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class PermissionRepository(
    private val context: Context,
    private val owner: LifecycleOwner,
) {
    private val _permissions = MutableStateFlow(readPermissions())
    val permissions: StateFlow<FlexRuntimePermissions> = _permissions.asStateFlow()

    init {
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                refreshPermissions()
            }
        })
    }

    fun refreshPermissions() {
        _permissions.value = readPermissions()
    }

    private fun readPermissions() = FlexRuntimePermissions(
        canReadAudioFiles = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE),
        canWriteAudioFiles = isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE),
    )

    private fun isPermissionGranted(permissionName: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permissionName
        ) == PackageManager.PERMISSION_GRANTED
}