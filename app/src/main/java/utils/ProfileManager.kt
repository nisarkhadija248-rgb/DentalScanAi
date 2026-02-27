package com.dentalscan.utils

import android.content.Context

object ProfileManager {

    private const val PREFS_NAME = "settings"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    private const val KEY_ACTIVE_PROFILE_NAME = "active_profile_name"
    private const val NO_PROFILE = -1L

    fun setActiveProfile(context: Context, profileId: Long, profileName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ACTIVE_PROFILE_ID, profileId)
            .putString(KEY_ACTIVE_PROFILE_NAME, profileName)
            .apply()
    }

    fun getActiveProfileId(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ACTIVE_PROFILE_ID, NO_PROFILE)
    }

    fun getActiveProfileName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_PROFILE_NAME, "Default User") ?: "Default User"
    }

    fun hasActiveProfile(context: Context): Boolean {
        return getActiveProfileId(context) != NO_PROFILE
    }

    fun clearActiveProfile(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_PROFILE_ID)
            .remove(KEY_ACTIVE_PROFILE_NAME)
            .apply()
    }
}