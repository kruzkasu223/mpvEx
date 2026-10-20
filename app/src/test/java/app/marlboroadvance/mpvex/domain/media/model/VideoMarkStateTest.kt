package app.marlboroadvance.mpvex.domain.media.model

import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import org.junit.Assert.*
import org.junit.Test

class VideoMarkStateTest {
  private val state = PlaybackStateEntity(
    mediaTitle = "video.mp4_123", lastPosition = 42, playbackSpeed = 1.5,
    sid = 2, secondarySid = 3, subDelay = 250, subSpeed = 1.1,
    aid = 4, audioDelay = 100, videoZoom = 1.2f, timeRemaining = 58,
    externalSubtitles = "content://subtitles/1", hasBeenWatched = true,
  )
  private val now = 1_800_000_000_000L
  private val recent = now / 1000L - 60L
  private val old = now / 1000L - 90L * 86_400L

  @Test fun newResetsProgressAndWatchedWithoutLosingPlayerPreferences() {
    val marked = state.withMark(VideoMark.NEW, 100)
    assertEquals(state.copy(lastPosition = 0, timeRemaining = 100, hasBeenWatched = false, manualMark = "NEW"), marked)
    assertTrue(marked.isNewVideo(old, 7, now))
  }

  @Test fun noneClearsLabelsEvenForRecentlyAddedVideos() {
    val marked = state.withMark(VideoMark.NONE, 100)
    assertEquals(0, marked.lastPosition)
    assertFalse(marked.hasBeenWatched)
    assertFalse(marked.isNewVideo(recent, 7, now))
  }

  @Test fun finishedRemainsWatchedAndRestartsFromBeginning() {
    val marked = state.withMark(VideoMark.FINISHED, 100)
    assertTrue(marked.hasBeenWatched)
    assertEquals(0, marked.lastPosition)
    assertEquals(0, marked.timeRemaining)
    assertFalse(marked.isNewVideo(recent, 7, now))
  }

  @Test fun lastPlayedPreservesResumePositionAndTrackSettings() {
    val marked = state.withMark(VideoMark.LAST_PLAYED, 100)
    assertEquals(state.copy(manualMark = "LAST_PLAYED", hasBeenWatched = false), marked)
    assertFalse(marked.isNewVideo(recent, 7, now))
  }

  @Test fun unmarkedFilesKeepTheExistingAgeBasedNewBehavior() {
    val noState: PlaybackStateEntity? = null
    assertTrue(noState.isNewVideo(recent, 7, now))
    assertFalse(noState.isNewVideo(old, 7, now))
    assertFalse(state.isNewVideo(recent, 7, now))
  }

  @Test fun unknownStoredMarkDoesNotCrashAndDoesNotMakePlayedVideosNew() {
    assertNull(VideoMark.fromStored("future-status"))
    assertNull(VideoMark.fromStored(null))
    assertFalse(state.copy(manualMark = "future-status").isNewVideo(recent, 7, now))
  }
}
