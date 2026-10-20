package app.marlboroadvance.mpvex.database.repository

import androidx.room.withTransaction
import app.marlboroadvance.mpvex.database.MpvExDatabase
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.database.entities.RecentlyPlayedEntity
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoMark
import app.marlboroadvance.mpvex.domain.media.model.withMark
import app.marlboroadvance.mpvex.utils.media.MediaIdentifier
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoMarkRepository(private val database: MpvExDatabase) {
  suspend fun commonMark(videos: List<Video>): VideoMark? = withContext(Dispatchers.IO) {
    val marks = videos.map { video ->
      VideoMark.fromStored(database.videoDataDao()
        .getVideoDataByTitle(MediaIdentifier.forLocalPath(video.path))?.manualMark)
    }.distinct()
    marks.singleOrNull()
  }

  /** One transaction for the entire selection: failures never leave a partially marked folder. */
  suspend fun mark(videos: List<Video>, mark: VideoMark) = withContext(Dispatchers.IO) {
    database.withTransaction {
      val timestamp = System.currentTimeMillis()
      videos.distinctBy { it.path }.forEachIndexed { index, video ->
        val key = MediaIdentifier.forLocalPath(video.path)
        val old = database.videoDataDao().getVideoDataByTitle(key) ?: PlaybackStateEntity(
          mediaTitle = key, lastPosition = 0, playbackSpeed = 1.0,
          sid = -1, subDelay = 0, subSpeed = 1.0, aid = -1, audioDelay = 0,
        )
        val duration = (video.duration / 1000L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        database.videoDataDao().upsert(old.withMark(mark, duration))
        database.recentlyPlayedDao().deleteByFilePath(video.path)
        if (mark == VideoMark.LAST_PLAYED) {
          database.recentlyPlayedDao().insert(RecentlyPlayedEntity(
            filePath = video.path, fileName = video.displayName, videoTitle = video.title,
            duration = video.duration, fileSize = video.size, width = video.width, height = video.height,
            timestamp = timestamp + index, launchSource = "video_list",
          ))
        }
      }
    }
    MediaLibraryEvents.notifyChanged()
  }
}
