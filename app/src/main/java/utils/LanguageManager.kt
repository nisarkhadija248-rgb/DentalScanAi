package com.dentalscan.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LanguageManager {

    private const val PREFS_NAME = "settings"
    private const val KEY_LANGUAGE = "urdu_language"

    fun setLocale(context: Context): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isUrdu = prefs.getBoolean(KEY_LANGUAGE, false)

        val locale = if (isUrdu) Locale("ur") else Locale.ENGLISH
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun getString(context: Context, englishText: String, urduText: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isUrdu = prefs.getBoolean(KEY_LANGUAGE, false)
        return if (isUrdu) urduText else englishText
    }

    fun isUrdu(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LANGUAGE, false)
    }
}