/**
 * Delog
 * Copyright (C) 2023 dmba.info
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package `in`.delog.ui.component.preview.videos

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File

@SuppressLint("UnsafeOptInUsageError")
class VideoCache {

    var exoPlayerCacheSize: Long = 150 * 1024 * 1024 // 90MB

    var leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize)

    lateinit var exoDatabaseProvider: StandaloneDatabaseProvider
    lateinit var simpleCache: SimpleCache

    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    @Synchronized
    fun initFileCache(context: Context) {
        exoDatabaseProvider = StandaloneDatabaseProvider(context)

        simpleCache = SimpleCache(
            File(context.cacheDir, "exoplayer"),
            leastRecentlyUsedCacheEvictor,
            exoDatabaseProvider
        )
    }

    // This method should be called when proxy setting changes.
    fun renewCacheFactory(client: OkHttpClient) {
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(client)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun get(client: OkHttpClient): CacheDataSource.Factory {
        // Renews the factory because OkHttpMight have changed.
        renewCacheFactory(client)

        return cacheDataSourceFactory
    }
}