package com.aegis.survival.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return AppSettings(
            emergencyMode = prefs.getBoolean(KEY_EMERGENCY, false),
            onlineProxyEnabled = prefs.getBoolean(KEY_ONLINE_PROXY, false),
            proxyBaseUrl = prefs.getString(KEY_PROXY_URL, "").orEmpty(),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_EMERGENCY, settings.emergencyMode)
            .putBoolean(KEY_ONLINE_PROXY, settings.onlineProxyEnabled)
            .putString(KEY_PROXY_URL, settings.proxyBaseUrl.trim())
            .putBoolean(KEY_HAPTICS, settings.hapticsEnabled)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "aegis_survival_prefs"
        private const val KEY_EMERGENCY = "emergency_mode"
        private const val KEY_ONLINE_PROXY = "online_proxy_enabled"
        private const val KEY_PROXY_URL = "proxy_base_url"
        private const val KEY_HAPTICS = "haptics_enabled"
    }
}
