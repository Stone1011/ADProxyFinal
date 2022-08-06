package ad.blocker.connect.settings

import ad.blocker.connect.model.Proxy

interface AppSettings {

    var lastUsedProxy: Proxy

    var themeMode: Int
}
