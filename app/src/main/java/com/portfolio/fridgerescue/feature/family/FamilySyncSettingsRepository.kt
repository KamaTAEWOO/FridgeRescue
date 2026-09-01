package com.portfolio.fridgerescue.feature.family

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class FamilySyncSettings(
    val serverBaseUrl: String = "http://10.0.2.2:8080",
    val accountId: String? = null,
    val accessToken: String? = null,
    val displayName: String? = null,
    val familyId: String? = null,
    val familyName: String? = null,
    val inviteCode: String? = null,
    val revision: Long = 0,
    val lastSyncedAtEpochMillis: Long? = null,
) {
    val isConnected: Boolean get() = !accountId.isNullOrBlank() && !accessToken.isNullOrBlank()
}

interface FamilySyncSettingsRepository {
    val settings: Flow<FamilySyncSettings>
    suspend fun saveAccount(settings: FamilySyncSettings)
    suspend fun saveFamily(familyId: String, familyName: String, inviteCode: String)
    suspend fun saveSync(revision: Long, syncedAtEpochMillis: Long)
    suspend fun clear()
}

private val Context.familyDataStore by preferencesDataStore(name = "family_sync_settings")

class DataStoreFamilySyncSettingsRepository(
    private val context: Context,
) : FamilySyncSettingsRepository {
    override val settings: Flow<FamilySyncSettings> = context.familyDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            FamilySyncSettings(
                serverBaseUrl = preferences[SERVER_URL] ?: "http://10.0.2.2:8080",
                accountId = preferences[ACCOUNT_ID],
                accessToken = preferences[ACCESS_TOKEN],
                displayName = preferences[DISPLAY_NAME],
                familyId = preferences[FAMILY_ID],
                familyName = preferences[FAMILY_NAME],
                inviteCode = preferences[INVITE_CODE],
                revision = preferences[REVISION] ?: 0,
                lastSyncedAtEpochMillis = preferences[LAST_SYNCED_AT],
            )
        }

    override suspend fun saveAccount(settings: FamilySyncSettings) {
        context.familyDataStore.edit { preferences ->
            preferences[SERVER_URL] = settings.serverBaseUrl
            preferences[ACCOUNT_ID] = requireNotNull(settings.accountId)
            preferences[ACCESS_TOKEN] = requireNotNull(settings.accessToken)
            preferences[DISPLAY_NAME] = requireNotNull(settings.displayName)
            preferences[FAMILY_ID] = requireNotNull(settings.familyId)
            preferences[FAMILY_NAME] = requireNotNull(settings.familyName)
            preferences[INVITE_CODE] = requireNotNull(settings.inviteCode)
            preferences[REVISION] = settings.revision
        }
    }

    override suspend fun saveFamily(familyId: String, familyName: String, inviteCode: String) {
        context.familyDataStore.edit {
            it[FAMILY_ID] = familyId
            it[FAMILY_NAME] = familyName
            it[INVITE_CODE] = inviteCode
            it[REVISION] = 0
        }
    }

    override suspend fun saveSync(revision: Long, syncedAtEpochMillis: Long) {
        context.familyDataStore.edit {
            it[REVISION] = revision
            it[LAST_SYNCED_AT] = syncedAtEpochMillis
        }
    }

    override suspend fun clear() {
        context.familyDataStore.edit { it.clear() }
    }

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_base_url")
        val ACCOUNT_ID = stringPreferencesKey("account_id")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val FAMILY_ID = stringPreferencesKey("family_id")
        val FAMILY_NAME = stringPreferencesKey("family_name")
        val INVITE_CODE = stringPreferencesKey("invite_code")
        val REVISION = longPreferencesKey("revision")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }
}
