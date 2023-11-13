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
        val SERVER_URL:  Preferences.Key<String> = stringPreferencesKey("invite_url")
        val TOR_SOCK_PROXY_PORT:  Preferences.Key<String> = stringPreferencesKey("tor_sock_proxy_port")
        val ALWAYS_TOR_PROXY:  Preferences.Key<String> = stringPreferencesKey("always_tor_proxy")
    }

    fun getData(key: Preferences.Key<String>): Flow<String?>  = context.dataStore.data
        .map { preferences ->
            preferences[key] ?: getAppDefault(key)
        }

    suspend fun saveData(key: Preferences.Key<String>, value: String) {
        if (value=="") return
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    private fun getAppDefault(key: Preferences.Key<String>): String? {
        return when (key) {
            SERVER_URL -> context.resources.getStringArray(R.array.default_servers)[0]
            TOR_SOCK_PROXY_PORT -> context.resources.getString(R.string.tor_sock_proxy_port)
            ALWAYS_TOR_PROXY -> context.resources.getString(R.string.always_use_tor_proxy)
            else -> null
        }
    }

    suspend fun reset() {
        for (k in arrayOf(SERVER_URL, TOR_SOCK_PROXY_PORT, ALWAYS_TOR_PROXY)) {
            getAppDefault(k)?.let { saveData(k, it) }
        }
    }

}
