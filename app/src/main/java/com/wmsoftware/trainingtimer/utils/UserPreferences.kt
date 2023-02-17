package com.wmsoftware.trainingtimer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    fun getUserTheme() = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("theme")]
    }

    suspend fun saveTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("theme")] = isDark
        }
    }
}