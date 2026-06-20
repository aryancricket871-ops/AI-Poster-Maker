package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {
    private val EXPIRY_KEY = longPreferencesKey("premium_expiry_date")

    val premiumExpiryDate: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[EXPIRY_KEY] ?: 0L
    }

    suspend fun setPremiumExpiryDate(expiryTimeMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[EXPIRY_KEY] = expiryTimeMillis
        }
    }
}
