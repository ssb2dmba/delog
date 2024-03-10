package `in`.delog.service.ssb

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import `in`.delog.MainApplication
import `in`.delog.db.SettingStore
import `in`.delog.db.SettingStore.Companion.TOR_SOCK_PROXY_PORT
import io.matthewnelson.kmp.tor.KmpTorLoaderAndroid
import io.matthewnelson.kmp.tor.TorConfigProviderAndroid
import io.matthewnelson.kmp.tor.common.address.PortProxy
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Option.AorDorPort
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Option.AorTorF
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Option.Time
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Option.TorF
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Setting.ConnectionPadding
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Setting.ConnectionPaddingReduced
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Setting.DormantCanceledByStartup
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Setting.DormantClientTimeout
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Setting.Ports
import io.matthewnelson.kmp.tor.manager.TorManager
import io.matthewnelson.kmp.tor.manager.TorServiceConfig
import io.matthewnelson.kmp.tor.manager.common.TorOperationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TorService(app: Application) {

    private val _connected = MutableStateFlow(false)

    private val providerAndroid by lazy {

        object : TorConfigProviderAndroid(app.applicationContext) {
            override fun provide(): TorConfig {
                return TorConfig.Builder {
                    // Set SOCKS5 port
                    val socks = Ports.Socks()
                    put(socks.set(AorDorPort.Value(PortProxy(torProxyPort))))
                    // reset our socks object to defaults
                    socks.setDefault()
                    // For Android, disabling & reducing connection padding is
                    // advisable to minimize mobile data usage.
                    put(ConnectionPadding().set(AorTorF.False))

                    put(ConnectionPaddingReduced().set(TorF.True))
                    // Tor default is 24h. Reducing to 1 min helps mitigate
                    // unnecessary mobile data usage.
                    put(DormantClientTimeout().set(Time.Minutes(10)))
                    // Tor defaults this setting to false which would mean if
                    // Tor goes dormant, the next time it is started it will still
                    // be in the dormant state and will not bootstrap until being
                    // set to "active". This ensures that if it is a fresh start,
                    // dormancy will be cancelled automatically.
                    put(DormantCanceledByStartup().set(TorF.True))
                }.build()
            }
        }
    }

    private val loaderAndroid by lazy {
        KmpTorLoaderAndroid(provider = providerAndroid)
    }

    private val manager: TorManager by lazy {
        TorManager.newInstance(
            application = app,
            loader = loaderAndroid,
            requiredEvents = null
        )
    }

    val torOperationManager: TorOperationManager get() = manager

    private val listener = TorListener()
    val status: LiveData<Int> get() = listener.status

    var torProxyPort = 9050  // TODO

    init {
        manager.debug(true)
        manager.addListener(listener)
        listener.addLine(
            TorServiceConfig.getMetaData(app.applicationContext).toString()
        )
    }

    fun start() {
        if (_connected.value == true) {
            return
        }
        torOperationManager.startQuietly()
        _connected.value = true
    }

    fun stop() {
        torOperationManager.stopQuietly()
        _connected.value = false
    }

}