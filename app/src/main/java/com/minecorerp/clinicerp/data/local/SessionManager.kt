package com.minecorerp.clinicerp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clinic_erp_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val SERVER_URL = stringPreferencesKey("server_url")
    private val DB_NAME = stringPreferencesKey("db_name")
    private val USER_LOGIN = stringPreferencesKey("user_login")
    private val USER_UID = longPreferencesKey("user_uid")
    private val PARTNER_ID = intPreferencesKey("partner_id")
    private val SESSION_ID = stringPreferencesKey("session_id")

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val dbName: Flow<String?> = context.dataStore.data.map { it[DB_NAME] }
    val userLogin: Flow<String?> = context.dataStore.data.map { it[USER_LOGIN] }
    val userUid: Flow<Long?> = context.dataStore.data.map { it[USER_UID] }
    val partnerId: Flow<Int?> = context.dataStore.data.map { it[PARTNER_ID] }
    val sessionId: Flow<String?> = context.dataStore.data.map { it[SESSION_ID] }

    suspend fun saveSession(url: String, db: String, login: String, uid: Long, sessionId: String?) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = url
            prefs[DB_NAME] = db
            prefs[USER_LOGIN] = login
            prefs[USER_UID] = uid
            prefs.remove(PARTNER_ID)
            prefs.remove(SESSION_ID)
            if (sessionId != null) prefs[SESSION_ID] = sessionId
        }
    }

    suspend fun savePartnerId(id: Int) {
        context.dataStore.edit { prefs ->
            prefs[PARTNER_ID] = id
        }
    }

    suspend fun updateSessionId(sessionId: String) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_ID] = sessionId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
