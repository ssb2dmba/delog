package `in`.delog.service.ssb

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import `in`.delog.MainApplication
import io.matthewnelson.kmp.tor.controller.common.events.TorEvent
import io.matthewnelson.kmp.tor.manager.common.event.TorManagerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

class TorListener : TorManagerEvent.Listener() {
    private val _eventLines: MutableLiveData<String> = MutableLiveData("")
    val eventLines: LiveData<String> = _eventLines
    private val events: MutableList<String> = ArrayList(50)

    fun addLine(line: String) {
        synchronized(this) {
            if (events.size > 49) {
                events.removeAt(0)
            }
            events.add(line)
            Log.d("TorListener", line)
            _eventLines.value = events.joinToString("\n")
        }
    }

    override fun onEvent(event: TorManagerEvent) {
        addLine(event.toString())

        super.onEvent(event)
    }

    override fun onEvent(event: TorEvent.Type.SingleLineEvent, output: String) {
        addLine("$event - $output")

        super.onEvent(event, output)
    }


    override fun onEvent(event: TorEvent.Type.MultiLineEvent, output: List<String>) {
        addLine("multi-line event: $event. See Logs.")

        // these events are many many many lines and should be moved
        // off the main thread if ever needed to be dealt with.
        MainApplication.getApplicationScope().launch(Dispatchers.IO) {
            Log.d("TorListener", "-------------- multi-line event START: $event --------------")
            for (line in output) {
                Log.d("TorListener", line)
            }
            Log.d("TorListener", "--------------- multi-line event END: $event ---------------")
        }

        super.onEvent(event, output)
    }

    override fun managerEventError(t: Throwable) {
        t.printStackTrace()
    }

    override fun managerEventAddressInfo(info: TorManagerEvent.AddressInfo) {
        if (info.isNull) {
            // Tear down HttpClient
        } else {
            info.socksInfoToProxyAddressOrNull()?.firstOrNull()?.let { proxyAddress ->
                @Suppress("UNUSED_VARIABLE")
                val proxy = InetSocketAddress(proxyAddress.address.value, proxyAddress.port.value)

                // Build HttpClient
            }
        }
    }

    override fun managerEventStartUpCompleteForTorInstance() {
        // Do one-time things after we're bootstrapped
        Log.d("TorListener", "Event StartUp Complete For Tor Instance")

    }
}