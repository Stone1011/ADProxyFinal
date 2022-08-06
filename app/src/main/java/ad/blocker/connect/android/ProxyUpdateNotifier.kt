package ad.blocker.connect.android

import ad.blocker.connect.broadcast.ProxyUpdateListenerProvider
import javax.inject.Inject

class ProxyUpdateNotifier @Inject constructor(
    private val listenerProvider: ProxyUpdateListenerProvider
) {

    fun notifyProxyChanged() {
        listenerProvider.listeners.forEach { it.onProxyUpdate() }
    }
}
