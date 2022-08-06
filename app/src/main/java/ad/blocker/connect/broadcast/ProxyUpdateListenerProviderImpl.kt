package ad.blocker.connect.broadcast

import ad.blocker.connect.feature.widget.broadcast.WidgetProxyUpdateListener
import javax.inject.Inject

class ProxyUpdateListenerProviderImpl @Inject constructor(
    private val widgetProxyUpdateListener: WidgetProxyUpdateListener
) : ProxyUpdateListenerProvider {

    override val listeners: List<ProxyUpdateListener>
        get() = listOf(widgetProxyUpdateListener)
}
