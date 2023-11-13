package `in`.delog.ui.component.preview.videos

import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import `in`.delog.MainApplication
import `in`.delog.service.HttpClient
import `in`.delog.ssb.BaseSsbService.Companion.TAG


@UnstableApi // Extend MediaSessionService
class PlaybackService : MediaSessionService() {
    private var videoViewedPositionCache = VideoViewedPositionCache()

    private var managerHls: MultiPlayerPlaybackManager? = null
    private var managerProgressive: MultiPlayerPlaybackManager? = null
    private var managerLocal: MultiPlayerPlaybackManager? = null

    fun newHslDataSource(): MediaSource.Factory {
        return HlsMediaSource.Factory(OkHttpDataSource.Factory(HttpClient.getHttpClient()))
    }

    fun newProgressiveDataSource(): MediaSource.Factory {
        return ProgressiveMediaSource.Factory(
            (applicationContext as MainApplication).videoCache.get(HttpClient.getHttpClient())
        )
    }

    fun lazyHlsDS(): MultiPlayerPlaybackManager {
        managerHls?.let { return it }

        val newInstance = MultiPlayerPlaybackManager(newHslDataSource(), videoViewedPositionCache)
        managerHls = newInstance
        return newInstance
    }

    fun lazyProgressiveDS(): MultiPlayerPlaybackManager {
        managerProgressive?.let { return it }

        val newInstance = MultiPlayerPlaybackManager(newProgressiveDataSource(), videoViewedPositionCache)
        managerProgressive = newInstance
        return newInstance
    }

    fun lazyLocalDS(): MultiPlayerPlaybackManager {
        managerLocal?.let { return it }

        val newInstance = MultiPlayerPlaybackManager(cachedPositions = videoViewedPositionCache)
        managerLocal = newInstance
        return newInstance
    }

    // Create your Player and MediaSession in the onCreate lifecycle event
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Stop all videos and recreates all managers when the proxy changes.
        HttpClient.proxyChangeListeners.add(this@PlaybackService::onProxyUpdated)
    }

    private fun onProxyUpdated() {
        val toDestroyHls = managerHls
        val toDestroyProgressive = managerProgressive

        managerHls = MultiPlayerPlaybackManager(newHslDataSource(), videoViewedPositionCache)
        managerProgressive = MultiPlayerPlaybackManager(newProgressiveDataSource(), videoViewedPositionCache)

        toDestroyHls?.releaseAppPlayers()
        toDestroyProgressive?.releaseAppPlayers()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        HttpClient.proxyChangeListeners.remove(this@PlaybackService::onProxyUpdated)

        managerHls?.releaseAppPlayers()
        managerLocal?.releaseAppPlayers()
        managerProgressive?.releaseAppPlayers()
        stopForeground(STOP_FOREGROUND_REMOVE)

        super.onDestroy()
    }

    fun getAppropriateMediaSessionManager(fileName: String): MultiPlayerPlaybackManager? {
        return if (fileName.startsWith("file")) {
            lazyLocalDS()
        } else if (fileName.endsWith("m3u8")) {
            lazyHlsDS()
        } else {
            lazyProgressiveDS()
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Updates any new player ready

        // Overrides the notification with any player actually playing
        managerHls?.playingContent()?.forEach {
            super.onUpdateNotification(session, startInForegroundRequired)
            Log.i("1HELLOOOOOO", "error:" + it.player.playerError?.message)
            if (it.player.isPlaying) {
                super.onUpdateNotification(it, startInForegroundRequired)
            }
        }
        managerLocal?.playingContent()?.forEach {
            Log.i("2HELLOOOOOO", "error:" + it.player.playerError?.message)
            if (it.player.isPlaying) {
                super.onUpdateNotification(session, startInForegroundRequired)
            }
        }
        managerProgressive?.playingContent()?.forEach {
            Log.i("3HELLOOOOOO", "error:" + it.player.playerError?.message)
            Log.i("4HELLOOOOOO", "error:" + session.player.playerError)
            if (it.player.isPlaying) {
                super.onUpdateNotification(session, startInForegroundRequired)
            }
        }

        // Overrides again with playing with audio
        managerHls?.playingContent()?.forEach {
            if (it.player.isPlaying && it.player.volume > 0) {
                super.onUpdateNotification(it, startInForegroundRequired)
            }
        }
        managerLocal?.playingContent()?.forEach {
            if (it.player.isPlaying && it.player.volume > 0) {
                super.onUpdateNotification(session, startInForegroundRequired)
            }
        }
        managerProgressive?.playingContent()?.forEach {
            if (it.player.isPlaying && it.player.volume > 0) {
                super.onUpdateNotification(session, startInForegroundRequired)
            }
        }
    }

    // Return a MediaSession to link with the MediaController that is making
    // this request.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val id = controllerInfo.connectionHints.getString("id") ?: return null
        val uri = controllerInfo.connectionHints.getString("uri") ?: return null
        val callbackUri = controllerInfo.connectionHints.getString("callbackUri")

        val manager = getAppropriateMediaSessionManager(uri)

        return manager?.getMediaSession(id, uri, callbackUri, context = this, applicationContext = applicationContext)
    }



}