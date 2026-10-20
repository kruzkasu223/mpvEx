package app.marlboroadvance.mpvex.domain.media.model

import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity

/** Preserve track, subtitle and speed preferences while changing the library status. */
fun PlaybackStateEntity.withMark(mark: VideoMark, durationSeconds: Int): PlaybackStateEntity = copy(
  manualMark = mark.name,
  lastPosition = if (mark == VideoMark.LAST_PLAYED) lastPosition else 0,
  timeRemaining = when (mark) {
    VideoMark.LAST_PLAYED -> timeRemaining
    VideoMark.FINISHED -> 0
    else -> durationSeconds
  },
  hasBeenWatched = mark == VideoMark.FINISHED,
)

fun PlaybackStateEntity?.isNewVideo(dateModifiedSeconds: Long, thresholdDays: Int, now: Long): Boolean =
  when (VideoMark.fromStored(this?.manualMark)) {
    VideoMark.NEW -> true
    VideoMark.LAST_PLAYED, VideoMark.FINISHED, VideoMark.NONE -> false
    null -> this == null && now - dateModifiedSeconds * 1000L <= thresholdDays * 86_400_000L
  }
