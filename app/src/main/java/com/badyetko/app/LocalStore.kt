package com.badyetko.app

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("badyetko_native", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun loadBudget(): BudgetState = runCatching {
        prefs.getString("budget", null)?.let { json.decodeFromString(BudgetState.serializer(), it) }
    }.getOrNull() ?: BudgetState()

    fun saveBudget(state: BudgetState) = prefs.edit().putString("budget", json.encodeToString(state)).apply()
    fun saveTheme(theme: AppTheme) = prefs.edit().putString("theme", theme.name).apply()
    fun loadTheme(): AppTheme = runCatching { AppTheme.valueOf(prefs.getString("theme", null) ?: "LIQUID_GLASS") }.getOrDefault(AppTheme.LIQUID_GLASS)
    fun saveSession(access: String, refresh: String) = prefs.edit().putString("access", access).putString("refresh", refresh).apply()
    fun accessToken(): String = prefs.getString("access", "") ?: ""
    fun refreshToken(): String = prefs.getString("refresh", "") ?: ""
    fun clearSession() = prefs.edit().remove("access").remove("refresh").apply()
}
