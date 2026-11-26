package com.aegis.survival.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryStatus(
    val isSystemPowerSave: Boolean = false,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
)

class BatteryStateMonitor(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val _status = MutableStateFlow(readCurrentStatus())
    val status: StateFlow<BatteryStatus> = _status.asStateFlow()

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            _status.value = readCurrentStatus(intent)
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        try {
            val sticky = context.registerReceiver(receiver, filter)
            registered = true
            _status.value = readCurrentStatus(sticky)
        } catch (_: Exception) {
            _status.value = readCurrentStatus()
        }
    }

    fun stop() {
        if (!registered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
            // Receiver already unregistered
        } finally {
            registered = false
        }
    }

    private fun readCurrentStatus(batteryIntent: Intent? = null): BatteryStatus {
        val powerSave = powerManager?.isPowerSaveMode == true

        val intent = batteryIntent ?: try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }

        val percent = if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) ((level * 100) / scale).coerceIn(0, 100)
            else batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        }

        val statusExtra = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusExtra == BatteryManager.BATTERY_STATUS_FULL

        return BatteryStatus(
            isSystemPowerSave = powerSave,
            batteryPercent = percent,
            isCharging = isCharging,
        )
    }
}
