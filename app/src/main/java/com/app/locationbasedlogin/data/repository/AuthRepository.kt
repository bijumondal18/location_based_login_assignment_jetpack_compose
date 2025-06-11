package com.app.locationbasedlogin.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import com.app.locationbasedlogin.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit

// DataStore instance (should be a singleton or injected in a real app)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class AuthRepository(private val context: Context) {

    private val IS_LOGGED_IN = booleanPreferencesKey(Constants.PREF_IS_LOGGED_IN)
    private val USER_LAT = doublePreferencesKey(Constants.PREF_USER_LAT)
    private val USER_LON = doublePreferencesKey(Constants.PREF_USER_LON)

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }

    suspend fun login(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_LAT] = latitude
            preferences[USER_LON] = longitude
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences.remove(USER_LAT)
            preferences.remove(USER_LON)
        }
    }

    suspend fun getUserLastKnownLocation(): Pair<Double, Double>? {
        val preferences = context.dataStore.data.first()
        val lat = preferences[USER_LAT]
        val lon = preferences[USER_LON]
        return if (lat != null && lon != null) lat to lon else null
    }

}