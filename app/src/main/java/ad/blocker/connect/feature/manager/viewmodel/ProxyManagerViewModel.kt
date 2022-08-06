package ad.blocker.connect.feature.manager.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import ad.blocker.connect.android.DeviceSettingsManager
import ad.blocker.connect.android.ProxyValidator
import ad.blocker.connect.android.ThemeSwitcher
import ad.blocker.connect.extensions.SingleLiveEvent
import ad.blocker.connect.feature.manager.view.ProxyManagerEvent
import ad.blocker.connect.feature.manager.view.ProxyState
import ad.blocker.connect.model.Proxy
import ad.blocker.connect.settings.AppSettings

class ProxyManagerViewModel @ViewModelInject constructor(
    private val deviceSettingsManager: DeviceSettingsManager,
    private val proxyValidator: ProxyValidator,
    private val appSettings: AppSettings,
    private val themeSwitcher: ThemeSwitcher
) : ViewModel() {

    val proxyEvent = SingleLiveEvent<ProxyManagerEvent>()

    val proxyState = Transformations.map(deviceSettingsManager.proxySetting) { proxy ->
        if (proxy.isEnabled) {
            ProxyState.Enabled(proxy.address, proxy.port)
        } else ProxyState.Disabled()
    }

    val lastUsedProxy: Proxy
        get() = appSettings.lastUsedProxy

    fun enableProxy(address: String, port: String) {
        when {
            !proxyValidator.isValidIP(address) -> {
                proxyEvent.value = ProxyManagerEvent.InvalidAddress
            }
            !proxyValidator.isValidPort(port) -> {
                proxyEvent.value = ProxyManagerEvent.InvalidPort
            }
            else -> {
                deviceSettingsManager.enableProxy(Proxy(address, port))
            }
        }
    }

    fun disableProxy() {
        deviceSettingsManager.disableProxy()
    }

    fun toggleTheme() = themeSwitcher.toggleTheme()
}
