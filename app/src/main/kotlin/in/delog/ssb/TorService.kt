package `in`.delog.ssb

import androidx.compose.ui.platform.LocalContext
import `in`.delog.MainApplication
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

class TorService {


    private val providerAndroid by lazy {

        object : TorConfigProviderAndroid(context = MainApplication.applicationContext()) {
            override fun provide(): TorConfig {
                return TorConfig.Builder {
                    // Set multiple ports for all of the things
                    val socks = Ports.Socks()
                    put(socks.set(AorDorPort.Value(PortProxy(9254))))
                    put(socks.set(AorDorPort.Value(PortProxy(9255))))
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

                    // If planning to use v3 Client Authentication in a persistent
                    // manner (where private keys are saved to disk via the "Persist"
                    // flag), this is needed to be set.
                    put(ClientOnionAuthDir().set(FileSystemDir(
                        workDir.builder { addSegment(ClientOnionAuthDir.DEFAULT_NAME) }
                    )))

                    val hsPath = workDir.builder {
                        addSegment(HiddenService.DEFAULT_PARENT_DIR_NAME)
                        addSegment("test_service")
                    }
                    // Add Hidden services
                    put(HiddenService()
                        .setPorts(ports = setOf(
                            // Use a unix domain socket to communicate via IPC instead of over TCP
                            HiddenService.UnixSocket(virtualPort = Port(80), targetUnixSocket = hsPath.builder {
                                addSegment(HiddenService.UnixSocket.DEFAULT_UNIX_SOCKET_NAME)
                            }),
                        ))
                        .setMaxStreams(maxStreams = HiddenService.MaxStreams(value = 2))
                        .setMaxStreamsCloseCircuit(value = TorF.True)
                        .set(FileSystemDir(path = hsPath))
                    )

                    put(HiddenService()
                        .setPorts(ports = setOf(
                            HiddenService.Ports(virtualPort = Port(80), targetPort = Port(1030)), // http
                            HiddenService.Ports(virtualPort = Port(443), targetPort = Port(1030)) // https
                        ))
                        .set(FileSystemDir(path =
                        workDir.builder {
                            addSegment(HiddenService.DEFAULT_PARENT_DIR_NAME)
                            addSegment("test_service_2")
                        }
                        ))
                    )
                }.build()
            }
        }
    }


}