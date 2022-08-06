package ad.blocker.connect.feature.manager.view

sealed class ProxyManagerEvent {
    object InvalidAddress : ProxyManagerEvent()
    object InvalidPort : ProxyManagerEvent()
}
