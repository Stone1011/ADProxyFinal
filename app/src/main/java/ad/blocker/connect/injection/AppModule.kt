package ad.blocker.connect.injection

import ad.blocker.connect.android.SharedPrefsAppSettings
import ad.blocker.connect.broadcast.ProxyUpdateListenerProvider
import ad.blocker.connect.broadcast.ProxyUpdateListenerProviderImpl
import ad.blocker.connect.settings.AppSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent

@Module
@InstallIn(ApplicationComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindProxyUpdateListenerProvider(
        providerImpl: ProxyUpdateListenerProviderImpl
    ): ProxyUpdateListenerProvider

    @Binds
    abstract fun bindAppSettings(
        sharedPrefsAppSettings: SharedPrefsAppSettings
    ): AppSettings
}
