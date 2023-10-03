package `in`.delog.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.delog.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
        val INVITE_URL:  Preferences.Key<String> = stringPreferencesKey("invite_url")
    }

    fun getData(key: Preferences.Key<String>): Flow<String?>  = context.dataStore.data
        .map { preferences ->
            preferences[key] ?: getAppDefault(key)
        }

    suspend fun saveData(key: Preferences.Key<String>, name: String) {
        context.dataStore.edit { preferences ->
            preferences[key] = name
        }
    }

    private fun getAppDefault(key: Preferences.Key<String>): String? {
        return when (key) {
            INVITE_URL -> context.getString(R.string.default_invite_url)
            else -> null
        }
    }

}