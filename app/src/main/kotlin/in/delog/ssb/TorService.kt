package `in`.delog.ssb

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import `in`.delog.MainApplication
import `in`.delog.R
import `in`.delog.db.SettingStore
import `in`.delog.db.SettingStore.Companion.TOR_SOCK_PROXY_PORT
import io.matthewnelson.kmp.tor.TorConfigProviderAndroid
import io.matthewnelson.kmp.tor.KmpTorLoaderAndroid
import io.matthewnelson.kmp.tor.common.address.*
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Setting.*
import io.matthewnelson.kmp.tor.controller.common.config.TorConfig.Option.*
import io.matthewnelson.kmp.tor.controller.common.control.usecase.TorControlInfoGet
import io.matthewnelson.kmp.tor.controller.common.events.TorEvent
import io.matthewnelson.kmp.tor.manager.TorManager
import io.matthewnelson.kmp.tor.manager.TorServiceConfig
import io.matthewnelson.kmp.tor.manager.common.TorControlManager
import io.matthewnelson.kmp.tor.manager.common.TorOperationManager
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TorService {

    private val providerAndroid by lazy {

        object : TorConfigProviderAndroid(context = MainApplication.applicationContext()) {
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
                    put(DormantClientTimeout().set(Time.Minutes(1)))
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
        TorManager.newInstance(application = MainApplication.applicationContext() as Application, loader = loaderAndroid, requiredEvents = null)
    }

    val torOperationManager: TorOperationManager get() = manager

    private val listener = TorListener()

    var torProxyPort = MainApplication.applicationContext().resources.getString(R.string.tor_sock_proxy_port).toInt()

    init {
        val store = SettingStore(MainApplication.applicationContext())

        MainApplication.getApplicationScope().launch {
            store.getData(TOR_SOCK_PROXY_PORT).collect {
                if (it !=null && it!!.toIntOrNull()!=null) {
                    torProxyPort = it.toInt()
                }
                torProxyPort = torProxyPort
                manager.debug(true)
                manager.addListener(listener)
                listener.addLine(TorServiceConfig.getMetaData(MainApplication.applicationContext()).toString())
            }
        }
    }

    suspend fun start() {
        torOperationManager.start();
    }

    fun stop() {
        MainApplication.getApplicationScope().launch {
            torOperationManager.stopQuietly()
        }
    }

}