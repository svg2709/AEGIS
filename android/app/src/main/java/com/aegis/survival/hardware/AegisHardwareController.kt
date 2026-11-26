package com.aegis.survival.hardware

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AegisHardwareController(private val context: Context) {

    private val cameraManager: CameraManager? =
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive.asStateFlow()

    private val _beaconLit = MutableStateFlow(false)
    val beaconLit: StateFlow<Boolean> = _beaconLit.asStateFlow()

    private val _morseStepIndex = MutableStateFlow(-1)
    val morseStepIndex: StateFlow<Int> = _morseStepIndex.asStateFlow()

    private val _usingScreenFallback = MutableStateFlow(false)
    val usingScreenFallback: StateFlow<Boolean> = _usingScreenFallback.asStateFlow()

    private var sosJob: Job? = null
    private var torchCameraId: String? = findFlashCameraId()

    fun hasFlashHardware(): Boolean = torchCameraId != null

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun vibrate(durationMs: Long, enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs.coerceIn(15L, 500L), VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.coerceIn(15L, 500L))
            }
        } catch (_: SecurityException) {
            // Ignore if vibration permission is restricted
        } catch (_: Exception) {
            // Ignore hardware errors
        }
    }

    fun copyToClipboard(label: String, text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (_: Exception) {
            // Ignore clipboard failure
        }
    }

    fun toggleSosBeacon(
        scope: CoroutineScope,
        screenOnly: Boolean = false,
        hapticsEnabled: Boolean = true,
    ) {
        if (_sosActive.value) {
            stopSosBeacon()
            vibrate(40L, hapticsEnabled)
        } else {
            startSosBeacon(scope, screenOnly, hapticsEnabled)
        }
    }

    fun startSosBeacon(
        scope: CoroutineScope,
        screenOnly: Boolean = false,
        hapticsEnabled: Boolean = true,
    ) {
        stopSosBeacon()
        _sosActive.value = true
        val canUseTorch = !screenOnly && torchCameraId != null
        _usingScreenFallback.value = !canUseTorch
        vibrate(120L, hapticsEnabled)

        sosJob = scope.launch(Dispatchers.Default) {
            try {
                while (isActive) {
                    val sequence = listOf(
                        200L to 200L, 200L to 200L, 200L to 450L,
                        600L to 200L, 600L to 200L, 600L to 450L,
                        200L to 200L, 200L to 200L, 200L to 1400L,
                    )
                    sequence.forEachIndexed { idx, (onMs, offMs) ->
                        if (!isActive) return@forEachIndexed
                        _morseStepIndex.value = idx
                        setBeaconState(true, canUseTorch)
                        delay(onMs)
                        setBeaconState(false, canUseTorch)
                        delay(offMs)
                    }
                }
            } finally {
                _morseStepIndex.value = -1
                setBeaconState(false, canUseTorch)
            }
        }
    }

    fun stopSosBeacon() {
        sosJob?.cancel()
        sosJob = null
        setTorchSafe(false)
        _morseStepIndex.value = -1
        _beaconLit.value = false
        _sosActive.value = false
        _usingScreenFallback.value = false
    }

    fun shareProtocol(title: String, body: String) {
        val shareText = buildString {
            appendLine("AEGIS Offline Survival Protocol")
            appendLine(title)
            appendLine("------------------------------")
            appendLine(body)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AEGIS: $title")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Survival Protocol").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun setBeaconState(lit: Boolean, tryTorch: Boolean) {
        _beaconLit.value = lit
        if (tryTorch) {
            val ok = setTorchSafe(lit)
            if (!ok) {
                _usingScreenFallback.value = true
            }
        }
    }

    private fun setTorchSafe(enabled: Boolean): Boolean {
        val camId = torchCameraId ?: return false
        return try {
            cameraManager?.setTorchMode(camId, enabled)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findFlashCameraId(): String? {
        val manager = cameraManager ?: return null
        return try {
            manager.cameraIdList.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }
}
