package `in`.delog.ui.component.preview.videos


import android.util.LruCache

class VideoViewedPositionCache {
    val cachedPosition = LruCache<String, Long>(100)

    fun add(uri: String, position: Long) {
        cachedPosition.put(uri, position)
    }

    fun get(uri: String): Long? {
        return cachedPosition.get(uri)
    }
}