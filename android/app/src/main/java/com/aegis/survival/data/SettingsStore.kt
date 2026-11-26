package com.aegis.survival.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val themeRaw = prefs.getString(KEY_THEME_MODE, DisplayThemeMode.AMOLED.name)
        val themeMode = DisplayThemeMode.entries.firstOrNull { it.name == themeRaw }
            ?: DisplayThemeMode.AMOLED
        val savedBookmarks = prefs.getStringSet(KEY_BOOKMARKS, emptySet())?.toSet() ?: emptySet()

        return AppSettings(
            emergencyMode = prefs.getBoolean(KEY_EMERGENCY, false),
            onlineProxyEnabled = prefs.getBoolean(KEY_ONLINE_PROXY, false),
            proxyBaseUrl = prefs.getString(KEY_PROXY_URL, "").orEmpty(),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            displayThemeMode = themeMode,
            autoBatterySaverAmoled = prefs.getBoolean(KEY_AUTO_BATTERY_SAVER, true),
            manualBatterySaver = prefs.getBoolean(KEY_MANUAL_BATTERY_SAVER, false),
            autoCheckUpdates = prefs.getBoolean(KEY_AUTO_UPDATES, true),
            readingFontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.88f, 1.32f),
            bookmarkedArticleIds = savedBookmarks,
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_EMERGENCY, settings.emergencyMode)
            .putBoolean(KEY_ONLINE_PROXY, settings.onlineProxyEnabled)
            .putString(KEY_PROXY_URL, settings.proxyBaseUrl.trim())
            .putBoolean(KEY_HAPTICS, settings.hapticsEnabled)
            .putString(KEY_THEME_MODE, settings.displayThemeMode.name)
            .putBoolean(KEY_AUTO_BATTERY_SAVER, settings.autoBatterySaverAmoled)
            .putBoolean(KEY_MANUAL_BATTERY_SAVER, settings.manualBatterySaver)
            .putBoolean(KEY_AUTO_UPDATES, settings.autoCheckUpdates)
            .putFloat(KEY_FONT_SCALE, settings.readingFontScale.coerceIn(0.88f, 1.32f))
            .putStringSet(KEY_BOOKMARKS, settings.bookmarkedArticleIds.toSet())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "aegis_survival_prefs"
        private const val KEY_EMERGENCY = "emergency_mode"
        private const val KEY_ONLINE_PROXY = "online_proxy_enabled"
        private const val KEY_PROXY_URL = "proxy_base_url"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_THEME_MODE = "display_theme_mode"
        private const val KEY_AUTO_BATTERY_SAVER = "auto_battery_saver_amoled"
        private const val KEY_MANUAL_BATTERY_SAVER = "manual_battery_saver"
        private const val KEY_AUTO_UPDATES = "auto_check_updates"
        private const val KEY_FONT_SCALE = "reading_font_scale"
        private const val KEY_BOOKMARKS = "bookmarked_article_ids"
    }
}
