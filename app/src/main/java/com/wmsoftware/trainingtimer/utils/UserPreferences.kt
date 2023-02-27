package com.wmsoftware.trainingtimer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import com.wmsoftware.trainingtimer.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "userPreferences")
class UserPreferences(private val context: Context) {

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

    fun getUserSoundEnable() = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("sound")]
    }

    fun getTypeSound() = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("type_sound")] ?: 0
    }

    fun getUserLanguage() = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("language")]
    }

    fun getUserPremium() = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("ads")]
    }

    fun getProfiles(): Flow<List<Profile>> {
        return dataStore.data.map { preferences ->
                val jsonString = preferences[stringPreferencesKey("profiles")] ?: "[]"
                Json.decodeFromString(jsonString)
        }
    }

    fun getCountdown() = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("countdown")] ?: 0
    }

    fun getPrepareTime() = dataStore.data.map { preferences ->
        preferences[intPreferencesKey("prepare")] ?: 0
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

    suspend fun saveSoundEnable(isEnable: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("sound")] = isEnable
        }
    }

    suspend fun saveTypeSound(sound: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("type_sound")] = sound
        }
    }

    suspend fun saveCountdown(time: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("countdown")] = time
        }
    }

    suspend fun savePrepareTime(time: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("prepare")] = time
        }
    }

    suspend fun saveLanguage(language: Int) {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("language")] = language
        }
    }

    suspend fun saveUserPremium(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("ads")] = isPremium
        }
    }

    suspend fun saveProfiles(perfiles: List<Profile>) {
        val jsonString = Json.encodeToString(perfiles)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("profiles")] = jsonString
        }
    }
}