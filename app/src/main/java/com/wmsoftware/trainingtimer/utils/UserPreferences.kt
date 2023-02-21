package com.wmsoftware.trainingtimer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "userPreferences")
class UserPreferences(private val context: Context){

    private val dataStore: DataStore<Preferences> by lazy {
        context.dataStore
    }

    /** GETTERS **/
    fun getUserTheme() = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("theme")]
    }

    fun getUserVibration() = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("vibration")]
    }

    fun getUserLanguage() = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("language")]
    }

    /** SETTERS **/
    suspend fun saveTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("theme")] = isDark
        }
    }

    suspend fun saveVibration(isVibrate: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("vibration")] = isVibrate
        }
    }

    suspend fun saveLanguage(language: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("language")] = language
        }
    }
}