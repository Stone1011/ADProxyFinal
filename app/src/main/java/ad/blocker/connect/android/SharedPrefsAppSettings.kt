package ad.blocker.connect.android

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import ad.blocker.connect.model.Proxy
import ad.blocker.connect.model.ProxyMapper
import ad.blocker.connect.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPrefsAppSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyMapper: ProxyMapper
) : AppSettings {

    companion object {
        private const val SHARED_PREF_NAME = "AppSettings"
        private const val PREF_PROXY = "proxy"
        private const val PREF_THEME = "theme"
    }

    private val prefs by lazy {
        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    override var lastUsedProxy: Proxy
        get() = proxyMapper.from(prefs.getString(PREF_PROXY, null))
        set(value) = prefs.edit { putString(PREF_PROXY, value.toString()) }

    override var themeMode: Int
        get() = prefs.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit { putInt(PREF_THEME, value) }
}
