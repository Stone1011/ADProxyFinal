package ad.blocker.connect.feature.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import ad.blocker.R
import ad.blocker.connect.android.DeviceSettingsManager
import ad.blocker.connect.model.Proxy
import ad.blocker.connect.settings.AppSettings
import ad.blocker.connect.view.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class ProxyTileService : TileService() {

    @Inject
    lateinit var deviceSettingsManager: DeviceSettingsManager

    @Inject
    lateinit var appSettings: AppSettings

    override fun onClick() {
        toggleProxy()
    }

    override fun onStartListening() {
        updateTile()
    }

    private fun toggleProxy() {
        val proxy = deviceSettingsManager.proxySetting.value ?: Proxy.Disabled
        if (proxy.isEnabled) {
            deviceSettingsManager.disableProxy()
        } else {
            val lastUsedProxy = appSettings.lastUsedProxy
            if (lastUsedProxy.isEnabled) {
                deviceSettingsManager.enableProxy(lastUsedProxy)
            } else {
                // There is no last used Proxy, prompt the user to create one
                startActivityAndCollapse(MainActivity.getIntent(baseContext).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
        updateTile()
    }

    private fun updateTile() {
        val proxy = deviceSettingsManager.proxySetting.value ?: Proxy.Disabled
        if (proxy.isEnabled) {
            qsTile.apply {
                label = proxy.toString()
                state = Tile.STATE_ACTIVE
                updateTile()
            }
        } else {
            qsTile.apply {
                label = getString(R.string.no_proxy_tile)
                state = Tile.STATE_INACTIVE
                updateTile()
            }
        }
    }
}
