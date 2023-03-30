package org.dlog.repository

import androidx.lifecycle.LiveData
import org.dlog.db.dao.FeedDao
import org.dlog.db.model.Feed

/**
 * Repository to provide Identity data
 */

interface FeedRepository {
  val feeds : LiveData<List<Feed>>
  val count : LiveData<Int>
  suspend fun addFeed(feed : Feed)
  suspend fun updateFeed(feed : Feed)
  suspend fun delFeed(feed : Feed)
  suspend fun getFeed(id: String): Feed
  suspend fun getFeedLive(id: String): LiveData<Feed>
  suspend fun getDefaultFeed(): Feed
}

class FeedRepositoryImpl(private val feedDao: FeedDao) : FeedRepository {

  override val feeds : LiveData<List<Feed>> = feedDao.getAll()

  val feed2 : LiveData<List<Feed>> = feedDao.getAll()

  override val count = feedDao.liveCount()

  override suspend fun addFeed(feed : Feed) {
    feedDao.insert(feed = feed)
  }

  override suspend fun updateFeed(feed : Feed) {
    feedDao.update(feed)
  }

  override suspend fun delFeed(feed : Feed) {
    feedDao.delete(feed)
  }

  override suspend fun getFeed(id: String): Feed {
    return feedDao.findById(id)
  }

  override suspend fun getFeedLive(id: String): LiveData<Feed> {
    return feedDao.findByIdLive(id)
  }

  override suspend fun getDefaultFeed(): Feed {
    return feedDao.getDefaultFeed()
  }

}
